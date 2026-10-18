package io.github.quizup.identity.infrastructure.in.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Connexion mobile avec un ID token Google obtenu par le SDK natif.
 */
public record LoginWithGoogleRequest(
        @NotBlank String idToken
) {
}
