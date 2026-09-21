package io.github.quizup.identity.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.quizup.identity.infrastructure.security.OAuth2AuthorizationObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * Persistance JDBC des autorisations/codes/tokens et consentements du serveur
 * d'autorisation (partagés par toutes les instances via PostgreSQL).
 *
 * <p>Le registre des clients n'est <b>pas</b> persisté : il est fourni par
 * l'auto-configuration Spring Boot à partir des propriétés standard
 * {@code spring.security.oauth2.authorizationserver.client.*} (
 * {@code InMemoryRegisteredClientRepository}). Les clients sont statiques et donc
 * identiques sur chaque instance — seul l'état dynamique (authorizations,
 * consentements, tokens) doit être partagé.</p>
 */
@Configuration
public class OAuth2PersistenceConfig {

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
}
