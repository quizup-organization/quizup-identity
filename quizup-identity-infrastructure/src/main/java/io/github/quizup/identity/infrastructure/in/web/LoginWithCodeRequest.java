package io.github.quizup.identity.infrastructure.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Connexion avec le code OTP reçu par email.
 */
public record LoginWithCodeRequest(
        @NotBlank @Email String email,
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "Le code doit contenir 6 chiffres")
        String code
) {
}
