package com.marcos.codereviewagent.domain.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record FileChange(
        String filename,
        String status,
        int additions,
        int deletions,
        String patch
) {}
