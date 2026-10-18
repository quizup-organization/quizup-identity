package io.github.quizup.identity.domain.model;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Règles pures des signalements de joueurs et des suspensions de compte.
 */
public interface UserReportRules {

    int MAX_COMMENT_LENGTH = 500;
    int MAX_NOTE_LENGTH = 500;
    int MAX_REASONS = UserReportReason.values().length;

    /** Identifiant déterministe du thread (joueur signalé × signalant). */
    static String threadId(String targetUserId, String reporterId) {
        String seed = Objects.requireNonNull(targetUserId, "targetUserId") + "|"
                + Objects.requireNonNull(reporterId, "reporterId");
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }

    static void validateReport(Set<UserReportReason> reasons, String comment) {
        if (reasons == null || reasons.isEmpty()) {
            throw new IllegalArgumentException("At least one report reason is required");
        }
        if (reasons.size() > MAX_REASONS) {
            throw new IllegalArgumentException("Too many report reasons");
        }
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Report comment is too long");
        }
    }

    static void validateNote(String note) {
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("Note is too long");
        }
    }

    static void validateSuspension(Instant suspendedAt, Instant suspendedUntil) {
        Objects.requireNonNull(suspendedAt, "suspendedAt");
        if (suspendedUntil != null && !suspendedUntil.isAfter(suspendedAt)) {
            throw new IllegalArgumentException("Suspension end must be in the future");
        }
    }

    static void validateNotSelf(String targetUserId, String reporterId) {
        if (Objects.equals(targetUserId, reporterId)) {
            throw new IllegalArgumentException("A player cannot report themselves");
        }
    }
}
