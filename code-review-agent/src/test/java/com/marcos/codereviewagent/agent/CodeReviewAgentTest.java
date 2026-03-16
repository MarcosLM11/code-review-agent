package com.marcos.codereviewagent.agent;

import com.marcos.codereviewagent.domain.model.CompletedReview;
import com.marcos.codereviewagent.domain.port.PullRequestProvider;
import com.marcos.codereviewagent.domain.service.ReviewFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static com.marcos.codereviewagent.fixtures.CodeAnalysisFixtures.aCodeAnalysis;
import static com.marcos.codereviewagent.fixtures.PrDiffFixtures.aPrDiff;
import static com.marcos.codereviewagent.fixtures.PullRequestInputFixtures.OWNER;
import static com.marcos.codereviewagent.fixtures.PullRequestInputFixtures.PR_NUMBER;
import static com.marcos.codereviewagent.fixtures.PullRequestInputFixtures.REPO;
import static com.marcos.codereviewagent.fixtures.PullRequestInputFixtures.aPullRequestInput;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CodeReviewAgentTest {

    @Mock
    private PullRequestProvider pullRequestProvider;

    @Mock
    private ReviewFormatter reviewFormatter;

    private CodeReviewAgent agent;

    @BeforeEach
    void setUp() {
        agent = new CodeReviewAgent(pullRequestProvider, reviewFormatter);
    }

    @Test
    void shouldFetchPrDiffFromProvider() {
        // given
        var input = aPullRequestInput();
        var expectedDiff = aPrDiff();
        given(pullRequestProvider.fetchPullRequest(OWNER, REPO, PR_NUMBER))
                .willReturn(expectedDiff);

        // when
        var result = agent.fetchPrDiff(input);

        // then
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expectedDiff);
    }

    @Test
    void shouldPostReviewAndReturnCompletedReview() {
        // given
        var input = aPullRequestInput();
        var diff = aPrDiff();
        var analysis = aCodeAnalysis();
        var formattedBody = "## AI Code Review\nFormatted content";

        given(reviewFormatter.format(diff, analysis)).willReturn(formattedBody);
        given(pullRequestProvider.postReview(OWNER, REPO, PR_NUMBER, formattedBody))
                .willReturn(true);

        // when
        var result = agent.postReview(input, diff, analysis);

        // then
        var expected = CompletedReview.builder()
                .pullRequest(input)
                .analysis(analysis)
                .reviewBody(formattedBody)
                .posted(true)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldReturnPostedFalseWhenGitHubPostFails() {
        // given
        var input = aPullRequestInput();
        var diff = aPrDiff();
        var analysis = aCodeAnalysis();
        var formattedBody = "review body";

        given(reviewFormatter.format(diff, analysis)).willReturn(formattedBody);
        given(pullRequestProvider.postReview(OWNER, REPO, PR_NUMBER, formattedBody))
                .willReturn(false);

        // when
        var result = agent.postReview(input, diff, analysis);

        // then
        assertThat(result.posted()).isFalse();
    }
}
