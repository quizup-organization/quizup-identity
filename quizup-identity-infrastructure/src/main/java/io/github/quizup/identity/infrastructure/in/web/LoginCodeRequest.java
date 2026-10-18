package io.github.quizup.identity.infrastructure.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Demande d'envoi d'un code de connexion (OTP email).
 *
 * <p>Le même body est utilisé par les endpoints web et mobile : le préfixe de la route
 * porte le client, pas le payload.</p>
 */
public record LoginCodeRequest(
        @NotBlank @Email String email
) {
}
