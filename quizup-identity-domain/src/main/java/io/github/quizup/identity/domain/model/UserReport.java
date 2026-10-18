package io.github.quizup.identity.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.Set;

/**
 * Signalement d'un joueur par un autre joueur.
 *
 * <p>Un <b>thread</b> regroupe les signalements successifs d'un même couple
 * (joueur signalé × signalant). Un seul signalement est {@code OPEN} à la fois.</p>
 */
@Builder(toBuilder = true)
public record UserReport(
        String reportId,
        String threadId,
        String targetUserId,
        String reporterId,
        Set<UserReportReason> reasons,
        String comment,
        UserReportContext context,
        String gameId,
        UserReportSnapshot snapshot,
        UserReportStatus status,
        Instant createdAt,
        String reviewedBy,
        Instant reviewedAt,
        String resolutionNote
) {

    public UserReport {
        reasons = reasons == null ? Set.of() : Set.copyOf(reasons);
    }

    public boolean open() {
        return status == UserReportStatus.OPEN;
    }
}
