package com.marcos.codereviewagent.application;

import com.marcos.codereviewagent.domain.port.PullRequestProvider;
import com.marcos.codereviewagent.domain.service.ReviewFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.marcos.codereviewagent.fixtures.PrDiffFixtures.aPrDiff;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    private static final String PR_OPENED_PAYLOAD = """
            {
              "action": "opened",
              "pull_request": {
                "number": 42,
                "title": "Fix bug"
              },
              "repository": {
                "name": "webapp",
                "owner": {
                  "login": "acme"
                }
              }
            }
            """;

    private static final String PR_CLOSED_PAYLOAD = """
            {
              "action": "closed",
              "pull_request": { "number": 42 },
              "repository": { "name": "webapp", "owner": { "login": "acme" } }
            }
            """;

    @Mock
    private PullRequestProvider pullRequestProvider;

    @Mock
    private ReviewFormatter reviewFormatter;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        var controller = new WebhookController(pullRequestProvider, reviewFormatter);
        client = WebTestClient.bindToController(controller).build();
    }

    @Test
    void shouldProcessOpenedPrAndPostReview() {
        // given
        var diff = aPrDiff();
        var reviewBody = "## PR Summary";

        given(pullRequestProvider.fetchPullRequest("acme", "webapp", 42))
                .willReturn(diff);
        given(reviewFormatter.formatSummary(diff))
                .willReturn(reviewBody);
        given(pullRequestProvider.postReview("acme", "webapp", 42, reviewBody))
                .willReturn(true);

        // when / then
        client.post()
                .uri("/api/v1/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .bodyValue(PR_OPENED_PAYLOAD)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("review posted");

        then(pullRequestProvider).should().postReview("acme", "webapp", 42, reviewBody);
    }

    @Test
    void shouldIgnoreNonPrEvents() {
        // when / then
        client.post()
                .uri("/api/v1/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "push")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("ignored");

        then(pullRequestProvider).should(never()).fetchPullRequest("acme", "webapp", 42);
    }

    @Test
    void shouldIgnoreClosedPrAction() {
        // when / then
        client.post()
                .uri("/api/v1/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .bodyValue(PR_CLOSED_PAYLOAD)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("ignored");

        then(pullRequestProvider).should(never()).fetchPullRequest("acme", "webapp", 42);
    }

    @Test
    void shouldReturnReviewFailedWhenPostFails() {
        // given
        var diff = aPrDiff();
        var reviewBody = "## PR Summary";

        given(pullRequestProvider.fetchPullRequest("acme", "webapp", 42))
                .willReturn(diff);
        given(reviewFormatter.formatSummary(diff))
                .willReturn(reviewBody);
        given(pullRequestProvider.postReview("acme", "webapp", 42, reviewBody))
                .willReturn(false);

        // when / then
        client.post()
                .uri("/api/v1/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .bodyValue(PR_OPENED_PAYLOAD)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("review failed");
    }
}
