package io.github.quizup.identity.infrastructure.in.web;

/**
 * Déconnexion : le refresh token éventuel permet de révoquer l'autorisation OAuth2
 * associée (usage unique, sans credential — un client public ne possède pas de secret).
 */
public record LogoutRequest(String refreshToken) {
}
