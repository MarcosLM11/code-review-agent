package com.marcos.codereviewagent.infraestructure;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@WireMockTest
class GitHubPullRequestAdapterTest {

    private GitHubPullRequestAdapter adapter;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        var webClient = WebClient.builder()
                .baseUrl(wmInfo.getHttpBaseUrl())
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .build();
        adapter = new GitHubPullRequestAdapter(webClient);
    }

    @Test
    void shouldFetchPullRequestMetadata() {
        // given
        stubFor(get(urlPathEqualTo("/repos/acme/app/pulls/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "title": "Fix bug",
                                  "body": "Fixes NPE",
                                  "base": {"ref": "main"},
                                  "head": {"ref": "fix/bug"}
                                }
                                """)));

        stubFor(get(urlPathEqualTo("/repos/acme/app/pulls/1/files"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [{
                                  "filename": "App.java",
                                  "status": "modified",
                                  "additions": 5,
                                  "deletions": 1,
                                  "patch": "@@ +null check"
                                }]
                                """)));

        // when
        var result = adapter.fetchPullRequest("acme", "app", 1);

        // then
        assertThat(result.title()).isEqualTo("Fix bug");
        assertThat(result.files()).hasSize(1);
        assertThat(result.files().getFirst().filename()).isEqualTo("App.java");
    }

    @Test
    void shouldPostReviewSuccessfully() {
        // given
        stubFor(post(urlPathEqualTo("/repos/acme/app/pulls/1/reviews"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1}")));

        // when
        var result = adapter.postReview("acme", "app", 1, "LGTM");

        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenPostFails() {
        // given
        stubFor(post(urlPathEqualTo("/repos/acme/app/pulls/1/reviews"))
                .willReturn(aResponse().withStatus(500)));

        // when
        var result = adapter.postReview("acme", "app", 1, "Review body");

        // then
        assertThat(result).isFalse();
    }
}
