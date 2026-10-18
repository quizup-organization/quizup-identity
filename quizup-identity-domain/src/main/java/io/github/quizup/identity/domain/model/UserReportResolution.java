package io.github.quizup.identity.domain.model;

/**
 * Décision de l'administration sur un signalement.
 */
public enum UserReportResolution {
    RESOLVED,
    DISMISSED;

    public UserReportStatus status() {
        return this == RESOLVED ? UserReportStatus.RESOLVED : UserReportStatus.DISMISSED;
    }
}
