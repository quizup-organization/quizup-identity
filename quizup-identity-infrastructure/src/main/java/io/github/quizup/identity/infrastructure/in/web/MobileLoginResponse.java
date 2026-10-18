package io.github.quizup.identity.infrastructure.in.web;

/**
 * Session mobile : identité + tokens OAuth2. Les tokens sont les mêmes que ceux du flux
 * OIDC (mêmes claims JWT, même refresh token, même rotation) ; seule la voie d'émission
 * change (pas de navigateur).
 */
public record MobileLoginResponse(
        String userId,
        String email,
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}
