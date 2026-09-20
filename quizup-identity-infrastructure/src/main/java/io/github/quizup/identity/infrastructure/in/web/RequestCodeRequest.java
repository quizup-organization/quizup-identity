package io.github.quizup.identity.infrastructure.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Demande d'envoi d'un code de connexion à usage unique.
 */
public record RequestCodeRequest(
        @NotBlank @Email String email
) {
}
