package io.github.quizup.identity.domain.port.out;

/**
 * Port sortant des KPI métier d'authentification.
 *
 * <p>Implémenté en infrastructure avec Micrometer ({@code MeterRegistry}). Aucun type technique
 * dans le domaine (règle hexagonale).
 */
public interface AuthMetricsPort {

    /** Authentification par mot de passe réussie. */
    void loginSucceeded();

    /** Authentification par mot de passe échouée (identifiants invalides). */
    void loginFailed();

    /**
     * Inscription réussie.
     *
     * @param method {@code PASSWORD} ou {@code SOCIAL}
     */
    void registered(String method);

    /**
     * Provider social lié à un compte existant.
     *
     * @param provider nom du provider (ex. {@code GOOGLE})
     */
    void socialProviderLinked(String provider);
}
