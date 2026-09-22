package io.github.quizup.identity.infrastructure.config;

import org.springframework.lang.Nullable;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Instant;
import java.util.Base64;

/**
 * Génère un refresh token opaque pour <b>tous</b> les clients, y compris les clients
 * publics ({@code client-authentication-methods: none}).
 *
 * <p>Spring Authorization Server refuse par défaut d'émettre un refresh token à un
 * client public sur le grant {@code authorization_code}
 * ({@code OAuth2RefreshTokenGenerator#isPublicClientForAuthorizationCodeGrant}). On
 * duplique ici la génération sans ce garde afin que les clients {@code web} et
 * {@code mobile} puissent renouveler leur session. La rotation reste pilotée par
 * {@code TokenSettings.reuseRefreshTokens=false}.</p>
 */
public final class PublicClientRefreshTokenGenerator implements OAuth2TokenGenerator<OAuth2RefreshToken> {

    private final StringKeyGenerator refreshTokenGenerator = new Base64StringKeyGenerator(
            Base64.getUrlEncoder().withoutPadding(), 96);

    @Override
    @Nullable
    public OAuth2RefreshToken generate(OAuth2TokenContext context) {
        if (!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())) {
            return null;
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt
                .plus(context.getRegisteredClient().getTokenSettings().getRefreshTokenTimeToLive());
        return new OAuth2RefreshToken(this.refreshTokenGenerator.generateKey(), issuedAt, expiresAt);
    }
}
