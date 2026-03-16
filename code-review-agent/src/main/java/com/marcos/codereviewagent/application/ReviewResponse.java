package com.marcos.codereviewagent.application;

import lombok.Builder;

@Builder
public record ReviewResponse(
        String owner,
        String repo,
        int prNumber,
        String title,
        int filesChanged,
        String reviewBody,
        boolean posted) {}
