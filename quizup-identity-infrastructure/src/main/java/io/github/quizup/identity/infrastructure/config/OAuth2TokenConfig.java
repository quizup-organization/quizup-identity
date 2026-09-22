package io.github.quizup.identity.infrastructure.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

/**
 * Générateur de tokens du serveur d'autorisation.
 *
 * <p>Reproduit le {@code DelegatingOAuth2TokenGenerator} par défaut de Spring
 * Authorization Server (JWT signés + générateur d'access token), mais remplace le
 * refresh token generator par {@link PublicClientRefreshTokenGenerator} afin d'émettre
 * des refresh tokens aux clients publics {@code web} et {@code mobile}.</p>
 *
 * <p>⚠️ Le customizer JWT ({@code JwtTokenCustomizer}) doit être rattaché explicitement
 * au {@link JwtGenerator} : le générateur par défaut du framework le fait, pas un
 * générateur fourni par l'application.</p>
 */
@Configuration(proxyBeanMethods = false)
public class OAuth2TokenConfig {

    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator(JWKSource<SecurityContext> jwkSource,
                                                  OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer) {
        JwtGenerator jwtGenerator = new JwtGenerator(new NimbusJwtEncoder(jwkSource));
        jwtGenerator.setJwtCustomizer(jwtCustomizer);

        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();

        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator,
                accessTokenGenerator,
                new PublicClientRefreshTokenGenerator());
    }
}
