package io.github.quizup.identity.domain.model;

/**
 * Origine d'une réintégration de compte.
 */
public enum SuspensionReinstatementReason {
    /** Réintégration manuelle par un administrateur. */
    MANUAL,
    /** Fin automatique d'une suspension temporaire. */
    EXPIRED
}
