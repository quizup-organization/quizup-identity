package io.github.quizup.identity.infrastructure.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Vérification d'un code de connexion à usage unique.
 */
public record VerifyCodeRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Le code doit contenir 6 chiffres") String code
) {
}
