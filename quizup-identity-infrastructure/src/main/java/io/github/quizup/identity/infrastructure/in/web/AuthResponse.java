package io.github.quizup.identity.infrastructure.in.web;

/**
 * Réponse de l'API JSON d'authentification. L'émission des tokens reste
 * exclusivement le rôle du pipeline Authorization Code + PKCE.
 */
public record AuthResponse(String userId, String email) {
}
