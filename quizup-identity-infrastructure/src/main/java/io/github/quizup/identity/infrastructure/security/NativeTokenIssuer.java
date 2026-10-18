package io.github.quizup.identity.infrastructure.security;

import io.github.quizup.microservice.core.domain.model.security.QuizUpPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Émet une session mobile (access token JWT + refresh token) sans passer par le navigateur.
 *
 * <p>Réutilise le {@link OAuth2TokenGenerator} et le {@link OAuth2AuthorizationService} du
 * serveur d'autorisation : les tokens sont strictement les mêmes que ceux du flux OIDC
 * (claims personnalisés, audience, rotation du refresh token, révocation). L'ID token
 * n'est pas émis : le mobile consomme l'access token JWT, et l'absence du scope
 * {@code openid} évite de devoir gérer nonce/session sur un flux natif.</p>
 */
@Component
public class NativeTokenIssuer {

    private static final Logger logger = LoggerFactory.getLogger(NativeTokenIssuer.class);

    /** Type d'autorisation dédié, persistant l'origine native de la session. */
    public static final AuthorizationGrantType NATIVE_GRANT_TYPE =
            new AuthorizationGrantType("urn:quizup:native");

    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2TokenGenerator<?> tokenGenerator;
    private final OAuth2AuthorizationService authorizationService;
    private final AuthorizationServerSettings authorizationServerSettings;

    public NativeTokenIssuer(RegisteredClientRepository registeredClientRepository,
                             OAuth2TokenGenerator<?> tokenGenerator,
                             OAuth2AuthorizationService authorizationService,
                             AuthorizationServerSettings authorizationServerSettings) {
        this.registeredClientRepository = registeredClientRepository;
        this.tokenGenerator = tokenGenerator;
        this.authorizationService = authorizationService;
        this.authorizationServerSettings = authorizationServerSettings;
    }

    public IssuedTokens issue(Authentication principal, String clientId) {
        if (!StringUtils.hasText(authorizationServerSettings.getIssuer())) {
            throw new IllegalStateException("Authorization server issuer is not configured");
        }

        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        if (client == null) {
            throw new IllegalStateException("Unknown OAuth2 client: " + clientId);
        }
        if (!client.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE)) {
            throw new IllegalStateException("Native token issuance requires a public client: " + clientId);
        }
        if (!client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            throw new IllegalStateException("Native token issuance requires refresh_token grant: " + clientId);
        }

        Set<String> scopes = client.getScopes().stream()
                .filter(scope -> !OidcScopes.OPENID.equals(scope))
                .collect(Collectors.toUnmodifiableSet());

        String principalName = principal.getPrincipal() instanceof QuizUpPrincipal quizUpPrincipal
                ? quizUpPrincipal.getUserId()
                : principal.getName();

        AuthorizationServerContext serverContext = new AuthorizationServerContext() {
            @Override
            public String getIssuer() {
                return authorizationServerSettings.getIssuer();
            }

            @Override
            public AuthorizationServerSettings getAuthorizationServerSettings() {
                return authorizationServerSettings;
            }
        };

        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(client)
                .principalName(principalName)
                .authorizationGrantType(NATIVE_GRANT_TYPE)
                .authorizedScopes(scopes)
                .attribute(Principal.class.getName(), principal);

        DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
                .registeredClient(client)
                .principal(principal)
                .authorizationServerContext(serverContext)
                .authorizedScopes(scopes)
                .authorizationGrantType(NATIVE_GRANT_TYPE);

        OAuth2Token generatedAccessToken = generateAccessToken(tokenContextBuilder);
        OAuth2AccessToken accessToken = toAccessToken(generatedAccessToken, scopes);
        authorizationBuilder.token(accessToken, metadata -> {
            if (generatedAccessToken instanceof Jwt jwt) {
                metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, jwt.getClaims());
            }
        });

        OAuth2RefreshToken refreshToken = generateRefreshToken(tokenContextBuilder);
        authorizationBuilder.refreshToken(refreshToken);

        authorizationService.save(authorizationBuilder.build());

        long expiresIn = accessToken.getIssuedAt() != null && accessToken.getExpiresAt() != null
                ? Duration.between(accessToken.getIssuedAt(), accessToken.getExpiresAt()).toSeconds()
                : 0;
        logger.info("Native tokens issued: userId={}, clientId={}", principalName, clientId);

        return new IssuedTokens(
                accessToken.getTokenValue(),
                refreshToken.getTokenValue(),
                OAuth2AccessToken.TokenType.BEARER.getValue(),
                expiresIn
        );
    }

    private OAuth2Token generateAccessToken(DefaultOAuth2TokenContext.Builder tokenContextBuilder) {
        OAuth2TokenContext context = tokenContextBuilder
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .build();
        OAuth2Token generated = tokenGenerator.generate(context);
        if (generated == null) {
            throw new IllegalStateException("The token generator failed to generate the access token");
        }
        return generated;
    }

    private OAuth2AccessToken toAccessToken(OAuth2Token generated, Set<String> scopes) {
        if (generated instanceof OAuth2AccessToken accessToken) {
            return accessToken;
        }
        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                generated.getTokenValue(),
                generated.getIssuedAt(),
                generated.getExpiresAt(),
                scopes
        );
    }

    private OAuth2RefreshToken generateRefreshToken(DefaultOAuth2TokenContext.Builder tokenContextBuilder) {
        OAuth2TokenContext context = tokenContextBuilder
                .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                .build();
        OAuth2Token generated = tokenGenerator.generate(context);
        if (!(generated instanceof OAuth2RefreshToken refreshToken)) {
            throw new IllegalStateException("The token generator failed to generate the refresh token");
        }
        return refreshToken;
    }

    /** Tokens émis par identity pour une session mobile. */
    public record IssuedTokens(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn
    ) {
    }
}
