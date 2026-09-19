package io.github.quizup.identity.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;

/**
 * Construit l'{@link ObjectMapper} utilisé par {@link JdbcOAuth2AuthorizationService} :
 * mêmes modules de sécurité que Spring Authorization Server (qui activent le
 * default typing et l'allowlist), plus les mixins de nos principals custom.
 * Sans ces mixins, l'allowlist Jackson rejette les classes QuizUp à la
 * désérialisation des {@code OAuth2Authorization}.
 */
public final class OAuth2AuthorizationObjectMapper {

    private OAuth2AuthorizationObjectMapper() {
    }

    public static ObjectMapper create() {
        ObjectMapper objectMapper = new ObjectMapper();

        ClassLoader classLoader = JdbcOAuth2AuthorizationService.class.getClassLoader();
        objectMapper.registerModules(SecurityJackson2Modules.getModules(classLoader));
        objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());

        objectMapper.addMixIn(UserPrincipal.class, UserPrincipalMixin.class);
        objectMapper.addMixIn(OidcUserPrincipal.class, OidcUserPrincipalMixin.class);

        return objectMapper;
    }
}
