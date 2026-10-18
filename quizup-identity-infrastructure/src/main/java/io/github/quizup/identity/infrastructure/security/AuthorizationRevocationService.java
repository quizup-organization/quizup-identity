package io.github.quizup.identity.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Service;

/**
 * Révocation d'une autorisation OAuth2 à partir de son refresh token.
 *
 * <p>Partagé par les logouts web et mobile : la session interactive (cookie) est propre
 * au web, mais la révocation du refresh token est commune.</p>
 */
@Service
public class AuthorizationRevocationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationRevocationService.class);

    private final OAuth2AuthorizationService authorizationService;

    public AuthorizationRevocationService(OAuth2AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public void revoke(String refreshToken) {
        OAuth2Authorization authorization =
                authorizationService.findByToken(refreshToken, OAuth2TokenType.REFRESH_TOKEN);
        if (authorization == null) {
            return;
        }
        authorizationService.remove(authorization);
        logger.info("Revoked OAuth2 authorization on logout: principal={}", authorization.getPrincipalName());
    }
}
