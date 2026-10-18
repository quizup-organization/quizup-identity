package io.github.quizup.identity.domain.model;

import java.util.List;

public record UserReportSummaryPage(
        List<UserReportSummary> content,
        long totalElements
) {
    public UserReportSummaryPage {
        content = content == null ? List.of() : List.copyOf(content);
    }
}
