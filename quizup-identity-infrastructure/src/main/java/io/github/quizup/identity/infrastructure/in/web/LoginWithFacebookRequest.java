package io.github.quizup.identity.infrastructure.in.web;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.util.StringUtils;

/**
 * Connexion mobile avec Facebook.
 *
 * <p>iOS (Limited Login) fournit un ID token OIDC + nonce ; Android (login classique)
 * fournit un access token Graph. Un seul des deux doit être présent.</p>
 */
public record LoginWithFacebookRequest(
        String idToken,
        String accessToken,
        String nonce
) {

    @AssertTrue(message = "idToken or accessToken is required")
    public boolean isTokenPresent() {
        return StringUtils.hasText(idToken) ^ StringUtils.hasText(accessToken);
    }

    @AssertTrue(message = "nonce is required with idToken")
    public boolean isNoncePresentWithIdToken() {
        return !StringUtils.hasText(idToken) || StringUtils.hasText(nonce);
    }
}
