package com.marcos.codereviewagent.domain.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record CompletedReview(
        PullRequestInput pullRequest,
        CodeAnalysis analysis,
        String reviewBody,
        boolean posted
) {}