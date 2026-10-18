package io.github.quizup.identity.domain.model;

/**
 * Motif d'une suspension de compte, choisi par l'administration.
 */
public enum UserSuspensionReason {
    HARASSMENT,
    HATE_SPEECH,
    THREATS,
    IMPERSONATION,
    PRIVACY_VIOLATION,
    INAPPROPRIATE_CONTENT,
    SPAM,
    COPYRIGHT,
    CHEATING,
    EXPLOIT_ABUSE,
    MULTIPLE_ACCOUNTS,
    TERMS_VIOLATION,
    MINOR_SAFETY,
    FRAUD,
    OTHER
}
