package com.marcos.codereviewagent.application;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReviewRequest(
        @NotBlank String owner,
        @NotBlank String repo,
        @Min(1) int prNumber) {}
