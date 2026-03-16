package com.marcos.codereviewagent.domain.model;

import lombok.Builder;
import java.util.List;

@Builder(toBuilder = true)
public record CodeAnalysis(
        List<CodeIssue> issues,
        List<ReviewSuggestion> suggestions,
        String overallAssessment
) {}
