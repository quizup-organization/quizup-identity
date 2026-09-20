package io.github.quizup.identity.domain.model;

import java.time.Instant;

/**
 * Code de connexion à usage unique (OTP email).
 *
 * <p>Le code en clair n'est jamais persisté : seul son hash l'est. Le record porte
 * l'état nécessaire pour appliquer les règles d'expiration et de tentatives.</p>
 */
public record LoginCode(
        String email,
        String codeHash,
        int attempts,
        Instant expiresAt,
        Instant createdAt,
        Instant consumedAt
) {

    public boolean isActive(Instant now) {
        return consumedAt == null && now.isBefore(expiresAt);
    }

    public boolean hasAttemptsLeft() {
        return attempts < LoginCodeRules.MAX_ATTEMPTS;
    }

    public LoginCode withFailedAttempt() {
        return new LoginCode(email, codeHash, attempts + 1, expiresAt, createdAt, consumedAt);
    }

    public LoginCode consume(Instant now) {
        return new LoginCode(email, codeHash, attempts, expiresAt, createdAt, now);
    }
}
