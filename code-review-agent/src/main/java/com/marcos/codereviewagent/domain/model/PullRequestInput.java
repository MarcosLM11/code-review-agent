package com.marcos.codereviewagent.domain.model;

public record PullRequestInput(
        String owner,
        String repo,
        int prNumber
) {}
