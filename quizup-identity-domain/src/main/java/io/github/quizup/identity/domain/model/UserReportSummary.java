package io.github.quizup.identity.domain.model;

import java.time.Instant;

/**
 * Ligne de la file admin, groupée par joueur signalé.
 */
public record UserReportSummary(
        String targetUserId,
        int openCount,
        int totalCount,
        Instant lastReportedAt
) {
}
