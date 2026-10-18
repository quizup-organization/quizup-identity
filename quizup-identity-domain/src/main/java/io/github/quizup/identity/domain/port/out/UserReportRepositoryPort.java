package io.github.quizup.identity.domain.port.out;

import io.github.quizup.identity.domain.model.UserReport;
import io.github.quizup.identity.domain.model.UserReportCount;
import io.github.quizup.identity.domain.model.UserReportSort;
import io.github.quizup.identity.domain.model.UserReportStatus;
import io.github.quizup.identity.domain.model.UserReportSummaryFilter;
import io.github.quizup.identity.domain.model.UserReportSummaryPage;

import java.util.List;
import java.util.Optional;

/**
 * Port sortant de persistance des signalements de joueurs.
 */
public interface UserReportRepositoryPort {

    void save(UserReport report);

    Optional<UserReport> findById(String reportId);

    List<UserReport> findByTargetUserId(String targetUserId);

    List<UserReport> findOpenByTargetUserId(String targetUserId);

    List<UserReportCount> findCountsByTargetUserIds(List<String> targetUserIds);

    UserReportSummaryPage findSummaryPage(UserReportSummaryFilter filter,
                                          UserReportSort sort,
                                          int page,
                                          int size);

    long countByStatus(UserReportStatus status);

    long countDistinctTargetUserIdByStatus(UserReportStatus status);
}
