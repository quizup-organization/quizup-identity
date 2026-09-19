package io.github.quizup.identity.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.quizup.identity.infrastructure.properties.OAuth2ClientsProperties;
import io.github.quizup.identity.infrastructure.security.OAuth2AuthorizationObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.util.Map;

/**
 * Persistance JDBC du serveur d'autorisation : clients, autorisations/codes/tokens
 * et consentements sont partagés par toutes les instances via PostgreSQL.
 * Le seed initial des clients provient de {@code authentication.oauth2.clients}.
 */
@Configuration
public class OAuth2PersistenceConfig {

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcOperations jdbcOperations) {
        return new JdbcRegisteredClientRepository(jdbcOperations);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcOperations jdbcOperations,
                                                           RegisteredClientRepository registeredClientRepository) {
        JdbcOAuth2AuthorizationService service =
                new JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository);

        ObjectMapper objectMapper = OAuth2AuthorizationObjectMapper.create();

        JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper rowMapper =
                new JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper(registeredClientRepository);
        rowMapper.setObjectMapper(objectMapper);
        service.setAuthorizationRowMapper(rowMapper);

        JdbcOAuth2AuthorizationService.OAuth2AuthorizationParametersMapper parametersMapper =
                new JdbcOAuth2AuthorizationService.OAuth2AuthorizationParametersMapper();
        parametersMapper.setObjectMapper(objectMapper);
        service.setAuthorizationParametersMapper(parametersMapper);

        return service;
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcOperations jdbcOperations,
                                                                         RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcOperations, registeredClientRepository);
    }

    @Bean
    public OAuth2ClientSeeder oAuth2ClientSeeder(RegisteredClientRepository registeredClientRepository,
                                                 OAuth2ClientsProperties clientsProperties,
                                                 PasswordEncoder passwordEncoder) {
        return new OAuth2ClientSeeder(registeredClientRepository, clientsProperties, passwordEncoder);
    }

    /**
     * Seed idempotent : chaque client de configuration est inséré/mis à jour
     * avec un id déterministe (= clientId). Le seed est donc rejouable sur N instances.
     */
    static final class OAuth2ClientSeeder implements ApplicationRunner {

        private static final Logger logger = LoggerFactory.getLogger(OAuth2ClientSeeder.class);

        private final RegisteredClientRepository registeredClientRepository;
        private final OAuth2ClientsProperties clientsProperties;
        private final PasswordEncoder passwordEncoder;

        OAuth2ClientSeeder(RegisteredClientRepository registeredClientRepository,
                           OAuth2ClientsProperties clientsProperties,
                           PasswordEncoder passwordEncoder) {
            this.registeredClientRepository = registeredClientRepository;
            this.clientsProperties = clientsProperties;
            this.passwordEncoder = passwordEncoder;
        }

        @Override
        public void run(ApplicationArguments args) {
            Map<String, OAuth2ClientsProperties.ClientConfig> clients = clientsProperties.getClients();

            if (clients == null || clients.isEmpty()) {
                logger.info("No OAuth2 clients configured (authentication.oauth2.clients is empty)");
                return;
            }

            clients.forEach((name, config) -> {
                RegisteredClient registeredClient = toRegisteredClient(name, config);
                registeredClientRepository.save(registeredClient);
                logger.info("Seeded OAuth2 client: id={}", registeredClient.getClientId());
            });
        }

        private RegisteredClient toRegisteredClient(String name, OAuth2ClientsProperties.ClientConfig config) {
            String clientId = config.getClientId() != null ? config.getClientId() : name;

            RegisteredClient.Builder builder = RegisteredClient.withId(clientId)
                    .clientId(clientId)
                    .clientName(clientId);

            if (config.getClientSecret() != null && !config.getClientSecret().isBlank()) {
                builder.clientSecret(passwordEncoder.encode(config.getClientSecret()));
            }

            config.getAuthenticationMethods()
                    .forEach(method -> builder.clientAuthenticationMethod(parseAuthenticationMethod(method)));
            config.getGrantTypes()
                    .forEach(grantType -> builder.authorizationGrantType(parseGrantType(grantType)));
            config.getScopes().forEach(builder::scope);
            config.getRedirectUris().forEach(builder::redirectUri);
            config.getPostLogoutRedirectUris().forEach(builder::postLogoutRedirectUri);

            builder.clientSettings(ClientSettings.builder()
                    .requireProofKey(config.isRequireProofKey())
                    .requireAuthorizationConsent(config.isRequireAuthorizationConsent())
                    .build());

            builder.tokenSettings(TokenSettings.builder()
                    .accessTokenTimeToLive(config.getTokenConfig().getAccessTokenTimeToLive())
                    .refreshTokenTimeToLive(config.getTokenConfig().getRefreshTokenTimeToLive())
                    .reuseRefreshTokens(config.getTokenConfig().isReuseRefreshTokens())
                    .build());

            return builder.build();
        }

        private ClientAuthenticationMethod parseAuthenticationMethod(String method) {
            return switch (method.toLowerCase()) {
                case "client_secret_post" -> ClientAuthenticationMethod.CLIENT_SECRET_POST;
                case "client_secret_jwt" -> ClientAuthenticationMethod.CLIENT_SECRET_JWT;
                case "private_key_jwt" -> ClientAuthenticationMethod.PRIVATE_KEY_JWT;
                case "none" -> ClientAuthenticationMethod.NONE;
                default -> ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
            };
        }

        private AuthorizationGrantType parseGrantType(String grantType) {
            return switch (grantType.toLowerCase()) {
                case "refresh_token" -> AuthorizationGrantType.REFRESH_TOKEN;
                case "client_credentials" -> AuthorizationGrantType.CLIENT_CREDENTIALS;
                default -> AuthorizationGrantType.AUTHORIZATION_CODE;
            };
        }
    }
}
