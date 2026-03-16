package com.marcos.codereviewagent.fixtures;

import com.marcos.codereviewagent.domain.model.CodeAnalysis;
import com.marcos.codereviewagent.domain.model.CodeIssue;
import com.marcos.codereviewagent.domain.model.ReviewSuggestion;
import java.util.List;
import static com.marcos.codereviewagent.domain.model.IssueCategory.STYLE;
import static com.marcos.codereviewagent.domain.model.IssueSeverity.INFO;

public final class CodeAnalysisFixtures {

    private CodeAnalysisFixtures() {}

    public static CodeIssue aStyleIssue() {
        return CodeIssue.builder()
                .filename("src/main/java/com/example/Service.java")
                .severity(INFO)
                .category(STYLE)
                .description("Consider using Optional instead of null check")
                .lineReference("line 12")
                .build();
    }

    public static ReviewSuggestion aSuggestion() {
        return ReviewSuggestion.builder()
                .filename("src/main/java/com/example/Service.java")
                .description("Use Optional.ofNullable for cleaner null handling")
                .suggestedCode("return Optional.ofNullable(config).map(Config::value);")
                .build();
    }

    public static CodeAnalysis aCodeAnalysis() {
        return CodeAnalysis.builder()
                .issues(List.of(aStyleIssue()))
                .suggestions(List.of(aSuggestion()))
                .overallAssessment("Minor style issue, otherwise solid. Ready to merge.")
                .build();
    }

    public static CodeAnalysis aCleanAnalysis() {
        return CodeAnalysis.builder()
                .issues(List.of())
                .suggestions(List.of())
                .overallAssessment("Code looks clean and well-structured. Approve.")
                .build();
    }
}