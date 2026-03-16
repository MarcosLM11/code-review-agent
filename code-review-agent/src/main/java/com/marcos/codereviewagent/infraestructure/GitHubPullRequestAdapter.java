package com.marcos.codereviewagent.infraestructure;

import com.marcos.codereviewagent.domain.model.FileChange;
import com.marcos.codereviewagent.domain.model.PrDiff;
import com.marcos.codereviewagent.domain.port.PullRequestProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
class GitHubPullRequestAdapter implements PullRequestProvider {

    private final WebClient gitHubWebClient;

    @Override
    public PrDiff fetchPullRequest(String owner, String repo, int prNumber) {
        log.info("Fetching PR #{} from {}/{}", prNumber, owner, repo);

        var prJson = gitHubWebClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}", owner, repo, prNumber)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        var filesJson = gitHubWebClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}/files", owner, repo, prNumber)
                .retrieve()
                .bodyToFlux(JsonNode.class)
                .collectList()
                .block();

        if (filesJson == null) {
            throw new IllegalStateException("Failed to fetch files for PR #" + prNumber + " on " + owner + "/" + repo);
        }
        
        List<FileChange> files = filesJson.stream()
                .map(this::toFileChange)
                .toList();

        var diffContent = gitHubWebClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}", owner, repo, prNumber)
                .accept(MediaType.valueOf("application/vnd.github.v3.diff"))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (prJson == null) {
            throw new IllegalStateException("Failed to fetch PR #" + prNumber + " on " + owner + "/" + repo);
        }
        
        return PrDiff.builder()
                .title(prJson.get("title").asString())
                .description(prJson.has("body") ? prJson.get("body").asString("") : "")
                .baseBranch(prJson.get("base").get("ref").asString())
                .headBranch(prJson.get("head").get("ref").asString())
                .diffContent(diffContent)
                .files(files)
                .build();
    }

    @Override
    public boolean postReview(String owner, String repo, int prNumber, String body) {
        log.info("Posting review to PR #{} on {}/{}", prNumber, owner, repo);
        try {
            gitHubWebClient.post()
                    .uri("/repos/{owner}/{repo}/pulls/{number}/reviews",
                            owner, repo, prNumber)
                    .bodyValue(Map.of("body", body, "event", "COMMENT"))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            return true;
        } catch (WebClientException e) {
            log.error("Failed to post review to PR #{} on {}/{}", prNumber, owner, repo, e);
            return false;
        }
    }

    private FileChange toFileChange(JsonNode node) {
        return FileChange.builder()
                .filename(node.get("filename").asString())
                .status(node.get("status").asString())
                .additions(node.get("additions").asInt())
                .deletions(node.get("deletions").asInt())
                .patch(node.has("patch") ? node.get("patch").asString() : null)
                .build();
    }
}
