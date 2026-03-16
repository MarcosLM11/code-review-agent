package com.marcos.codereviewagent.application;

import com.marcos.codereviewagent.domain.port.PullRequestProvider;
import com.marcos.codereviewagent.domain.service.ReviewFormatter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final PullRequestProvider pullRequestProvider;
    private final ReviewFormatter reviewFormatter;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {
        var diff = pullRequestProvider.fetchPullRequest(
                request.owner(), request.repo(), request.prNumber());

        var reviewBody = reviewFormatter.formatSummary(diff);

        var posted = pullRequestProvider.postReview(
                request.owner(), request.repo(), request.prNumber(), reviewBody);

        var response = ReviewResponse.builder()
                .owner(request.owner())
                .repo(request.repo())
                .prNumber(request.prNumber())
                .title(diff.title())
                .filesChanged(diff.files().size())
                .reviewBody(reviewBody)
                .posted(posted)
                .build();

        return ResponseEntity.ok(response);
    }
}
