package com.marcos.codereviewagent.shell;

import com.marcos.codereviewagent.domain.port.PullRequestProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@Slf4j
@ShellComponent
@RequiredArgsConstructor
public class ReviewCommands {

    private final PullRequestProvider pullRequestProvider;

    @ShellMethod(key = "fetch-pr", value = "Fetch a PR diff and display a summary")
    public String fetchPr(
            @ShellOption(help = "Repository owner") String owner,
            @ShellOption(help = "Repository name") String repo,
            @ShellOption(help = "Pull request number") int prNumber) {

        log.info("Fetching PR #{} from {}/{}", prNumber, owner, repo);
        var diff = pullRequestProvider.fetchPullRequest(owner, repo, prNumber);

        var sb = new StringBuilder();
        sb.append("\n--- PR #%d: %s ---\n".formatted(prNumber, diff.title()));
        sb.append("Description: %s\n".formatted(diff.description()));
        sb.append("Branch: %s → %s\n".formatted(diff.headBranch(), diff.baseBranch()));
        sb.append("Files changed: %d\n\n".formatted(diff.files().size()));

        for (var file : diff.files()) {
            sb.append("  %-50s [%-8s] +%d/-%d\n".formatted(
                    file.filename(), file.status(), file.additions(), file.deletions()));
        }

        return sb.toString();
    }
}
