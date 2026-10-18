package io.github.quizup.identity.infrastructure.in.web;

/**
 * Résultat d'une connexion web : la session interactive est portée par le cookie
 * {@code AUTH_TX}, le body ne contient que l'identité immuable de l'utilisateur.
 */
public record WebLoginResponse(String userId, String email) {
}
