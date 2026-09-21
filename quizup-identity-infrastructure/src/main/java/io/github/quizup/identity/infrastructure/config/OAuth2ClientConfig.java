package io.github.quizup.identity.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.server.servlet.OAuth2AuthorizationServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Registre des clients du serveur d'autorisation, construit à partir des propriétés
 * **standard** Spring Boot {@code spring.security.oauth2.authorizationserver.client.*}.
 *
 * <p>Pourquoi un bean maison alors que Boot en fournit un ? Boot stocke le
 * {@code client-secret} <b>brut</b> ; or Spring Security utilise le {@link PasswordEncoder}
 * de l'application (BCrypt) pour vérifier le secret des clients confidentiels
 * ({@code server}, {@code grafana}). On réencode donc le secret ici, comportement
 * identique au seeder précédent.</p>
 *
 * <p>Le registre est <b>in-memory</b> : les clients sont statiques et identiques sur
 * chaque instance ; seuls authorizations/consentements/tokens sont persistés en JDBC
 * ({@link OAuth2PersistenceConfig}).</p>
 */
@Configuration(proxyBeanMethods = false)
public class OAuth2ClientConfig {

    @Bean
    @ConditionalOnMissingBean(RegisteredClientRepository.class)
    public RegisteredClientRepository registeredClientRepository(OAuth2AuthorizationServerProperties properties,
                                                                 PasswordEncoder passwordEncoder) {
        List<RegisteredClient> clients = properties.getClient().entrySet().stream()
                .map(entry -> toRegisteredClient(entry.getKey(), entry.getValue(), passwordEncoder))
                .toList();
        return new InMemoryRegisteredClientRepository(clients);
    }

    private RegisteredClient toRegisteredClient(String registrationId,
                                                OAuth2AuthorizationServerProperties.Client client,
                                                PasswordEncoder passwordEncoder) {
        OAuth2AuthorizationServerProperties.Registration registration = client.getRegistration();
        OAuth2AuthorizationServerProperties.Token token = client.getToken();

        RegisteredClient.Builder builder = RegisteredClient.withId(registrationId)
                .clientId(registration.getClientId())
                .clientName(StringUtils.hasText(registration.getClientName())
                        ? registration.getClientName()
                        : registration.getClientId());

        if (StringUtils.hasText(registration.getClientSecret())) {
            builder.clientSecret(passwordEncoder.encode(registration.getClientSecret()));
        }

        registration.getClientAuthenticationMethods()
                .forEach(method -> builder.clientAuthenticationMethod(new ClientAuthenticationMethod(method)));
        registration.getAuthorizationGrantTypes()
                .forEach(grant -> builder.authorizationGrantType(new AuthorizationGrantType(grant)));
        registration.getRedirectUris().forEach(builder::redirectUri);
        registration.getPostLogoutRedirectUris().forEach(builder::postLogoutRedirectUri);
        registration.getScopes().forEach(builder::scope);

        builder.clientSettings(ClientSettings.builder()
                .requireProofKey(client.isRequireProofKey())
                .requireAuthorizationConsent(client.isRequireAuthorizationConsent())
                .build());

        builder.tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(token.getAccessTokenTimeToLive())
                .refreshTokenTimeToLive(token.getRefreshTokenTimeToLive())
                .reuseRefreshTokens(token.isReuseRefreshTokens())
                .build());

        return builder.build();
    }
}
