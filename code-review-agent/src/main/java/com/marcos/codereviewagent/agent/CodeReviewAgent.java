package com.marcos.codereviewagent.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Export;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.domain.io.UserInput;
import com.marcos.codereviewagent.domain.model.CodeAnalysis;
import com.marcos.codereviewagent.domain.model.CompletedReview;
import com.marcos.codereviewagent.domain.model.PrDiff;
import com.marcos.codereviewagent.domain.model.PullRequestInput;
import com.marcos.codereviewagent.domain.port.PullRequestProvider;
import com.marcos.codereviewagent.domain.service.ReviewFormatter;
import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;
import static com.marcos.codereviewagent.agent.ReviewPersonas.CODE_REVIEWER;

@Agent(description = "Analyze a GitHub pull request: fetch diff, find issues, post review")
@RequiredArgsConstructor
public class CodeReviewAgent {

    private final PullRequestProvider pullRequestProvider;
    private final ReviewFormatter reviewFormatter;

    @Action
    public PullRequestInput parsePrInput(UserInput userInput, Ai ai) {
        return ai
                .withAutoLlm()
                .creating(PullRequestInput.class)
                .fromPrompt("""
                        Extract the GitHub pull request details from this input.
                        The user may provide a URL like https://github.com/owner/repo/pull/123
                        or text like "review PR #42 in owner/repo".
                        Return owner, repo, and prNumber.

                        User input: %s
                        """.formatted(userInput.getContent()));
    }

    @Action
    public PrDiff fetchPrDiff(PullRequestInput input) {
        return pullRequestProvider.fetchPullRequest(
                input.owner(), input.repo(), input.prNumber());
    }

    @Action
    public CodeAnalysis analyzeCode(PrDiff diff, Ai ai) {
        var filePatches = diff.files().stream()
                .filter(f -> f.patch() != null)
                .map(f -> "### %s (%s, +%d/-%d)\n```diff\n%s\n```".formatted(
                        f.filename(), f.status(), f.additions(), f.deletions(),
                        truncate(f.patch(), 3000)))
                .collect(Collectors.joining("\n\n"));

        return ai
                .withAutoLlm()
                .withPromptContributor(CODE_REVIEWER)
                .creating(CodeAnalysis.class)
                .fromPrompt("""
                        Analyze the following pull request diff and identify issues
                        and suggest improvements.

                        ## PR: %s
                        %s

                        ## File Changes
                        %s

                        ## Instructions
                        For each issue, provide:
                        - filename and approximate line reference
                        - severity: CRITICAL (bugs, security), WARNING (performance,
                          logic), or INFO (style, naming)
                        - category: BUG, SECURITY, PERFORMANCE, STYLE, or MAINTAINABILITY
                        - clear description of the problem

                        For suggestions, provide actionable improvements with code
                        snippets where helpful.

                        End with an overall assessment paragraph summarizing PR quality
                        and whether it looks ready to merge.

                        Focus on real problems — do NOT invent issues. If the code
                        looks good, say so. Be concise and constructive.
                        """.formatted(diff.title(), diff.description(), filePatches));
    }

    @AchievesGoal(
            description = "A completed code review has been analyzed and posted to the pull request",
            export = @Export(remote = true, name = "prCodeReview")
    )
    @Action
    public CompletedReview postReview(
            PullRequestInput input,
            PrDiff diff,
            CodeAnalysis analysis) {

        var reviewBody = reviewFormatter.format(diff, analysis);

        var posted = pullRequestProvider.postReview(
                input.owner(), input.repo(), input.prNumber(), reviewBody);

        return CompletedReview.builder()
                .pullRequest(input)
                .analysis(analysis)
                .reviewBody(reviewBody)
                .posted(posted)
                .build();
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "\n... (truncated)";
    }
}
