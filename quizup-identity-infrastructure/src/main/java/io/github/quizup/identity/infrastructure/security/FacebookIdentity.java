package io.github.quizup.identity.infrastructure.security;

/** Identité Facebook vérifiée (subject app-scoped + email). */
public record FacebookIdentity(String subject, String email) {
}
