package io.github.quizup.identity.infrastructure.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for OAuth2 clients
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "authentication.oauth2")
public class OAuth2ClientsProperties {

    private Map<String, ClientConfig> clients;

    @Setter
    @Getter
    public static class ClientConfig {
        private String clientId;
        private String clientSecret;
        private List<String> authenticationMethods = List.of("client_secret_basic");
        private List<String> grantTypes = List.of("authorization_code", "refresh_token");
        private List<String> scopes = List.of("openid", "profile");
        private List<String> redirectUris = List.of();
        private List<String> postLogoutRedirectUris = List.of();
        private boolean requireProofKey = false;
        private boolean requireAuthorizationConsent = false;
        private TokenConfig tokenConfig = new TokenConfig();

    }

    @Setter
    @Getter
    public static class TokenConfig {
        private Duration accessTokenTimeToLive = Duration.ofMinutes(30);
        private Duration refreshTokenTimeToLive = Duration.ofDays(7);
        private boolean reuseRefreshTokens = false;

    }
}
