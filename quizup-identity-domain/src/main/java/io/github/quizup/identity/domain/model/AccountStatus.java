package io.github.quizup.identity.domain.model;

/**
 * Statut d'un compte QuizUp.
 *
 * <p>Le statut est porté par l'identité : c'est le seul service capable de bloquer
 * l'authentification (OTP, social, refresh token) et de révoquer les sessions.</p>
 */
public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    DELETED
}
