package io.github.quizup.identity.domain.query;

import io.github.quizup.identity.domain.model.UserReportSort;
import io.github.quizup.identity.domain.model.UserReportSummaryFilter;

public interface UserReportQuery {

    record GetUserReportByIdQuery(String reportId) {
    }

    record GetUserReportsByTargetQuery(String targetUserId) {
    }

    record GetUserReportCountsQuery(java.util.List<String> targetUserIds) {
    }

    record GetReportedUserPageQuery(
            UserReportSummaryFilter filter,
            UserReportSort sort,
            int page,
            int size
    ) {
    }

    record GetUserReportStatsQuery() {
    }
}
