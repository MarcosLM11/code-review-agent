package com.marcos.codereviewagent.domain;

import com.marcos.codereviewagent.domain.service.ReviewFormatter;
import org.junit.jupiter.api.Test;
import static com.marcos.codereviewagent.fixtures.CodeAnalysisFixtures.aCleanAnalysis;
import static com.marcos.codereviewagent.fixtures.CodeAnalysisFixtures.aCodeAnalysis;
import static com.marcos.codereviewagent.fixtures.PrDiffFixtures.aPrDiff;
import static org.assertj.core.api.Assertions.assertThat;

class ReviewFormatterTest {

    private final ReviewFormatter formatter = new ReviewFormatter();

    @Test
    void shouldFormatReviewWithIssuesAndSuggestions() {
        // given
        var diff = aPrDiff();
        var analysis = aCodeAnalysis();

        // when
        var result = formatter.format(diff, analysis);

        // then
        assertThat(result)
                .contains("## AI Code Review: Fix null pointer")
                .contains("[INFO]")
                .contains("Service.java")
                .contains("Optional")
                .contains("Ready to merge");
    }

    @Test
    void shouldFormatCleanReviewWithoutIssueSection() {
        // given
        var diff = aPrDiff();
        var analysis = aCleanAnalysis();

        // when
        var result = formatter.format(diff, analysis);

        // then
        assertThat(result)
                .contains("## AI Code Review")
                .contains("Approve")
                .doesNotContain("### Issues Found")
                .doesNotContain("### Suggestions");
    }

    @Test
    void shouldFormatSummaryWithFileList() {
        // given
        var diff = aPrDiff();

        // when
        var result = formatter.formatSummary(diff);

        // then
        assertThat(result)
                .contains("## PR Summary: Fix null pointer")
                .contains("fix/npe-merchant-config")
                .contains("main")
                .contains("Files Changed (2)")
                .contains("Service.java")
                .contains("ServiceTest.java");
    }

    @Test
    void shouldIncludeSuggestedCodeSnippets() {
        // given
        var diff = aPrDiff();
        var analysis = aCodeAnalysis();

        // when
        var result = formatter.format(diff, analysis);

        // then
        assertThat(result)
                .contains("Optional.ofNullable(config)")
                .contains("```");
    }
}
