package io.github.quizup.identity.domain.model;

/**
 * Statistiques de modération des comptes.
 */
public record UserReportStats(
        long openReports,
        long reportedUsers,
        long suspendedUsers
) {
}
