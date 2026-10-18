package io.github.quizup.identity.domain.model;

/**
 * Motif d'une suspension de compte, choisi par l'administration.
 */
public enum UserSuspensionReason {
    HARASSMENT,
    HATE_SPEECH,
    INAPPROPRIATE_CONTENT,
    CHEATING,
    SPAM,
    OTHER
}
