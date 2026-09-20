package io.github.quizup.identity.domain.model;

import java.time.Duration;

/**
 * Règles métier du code de connexion à usage unique (OTP email).
 */
public final class LoginCodeRules {

    /** Nombre de chiffres du code. */
    public static final int CODE_LENGTH = 6;

    /** Durée de validité d'un code. */
    public static final Duration TTL = Duration.ofMinutes(10);

    /** Nombre maximal de tentatives de vérification par code. */
    public static final int MAX_ATTEMPTS = 5;

    /** Délai minimal entre deux demandes de code pour un même email. */
    public static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private LoginCodeRules() {
    }
}
