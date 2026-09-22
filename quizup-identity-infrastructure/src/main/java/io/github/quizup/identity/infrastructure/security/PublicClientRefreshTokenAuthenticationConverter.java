package io.github.quizup.identity.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentifie un client public ({@code client-authentication-methods: none}) lors du grant
 * {@code refresh_token}.
 *
 * <p>Le converter Spring Authorization Server par défaut
 * ({@code PublicClientAuthenticationConverter}) ne matche que les requêtes
 * {@code authorization_code} + {@code code_verifier} (PKCE) : un échange de refresh token
 * par un client public n'est donc jamais authentifié. Ce converter comble ce trou en
 * produisant un {@link OAuth2ClientAuthenticationToken} de méthode {@code none} pour les
 * requêtes {@code grant_type=refresh_token} sans secret ni assertion client.</p>
 */
public final class PublicClientRefreshTokenAuthenticationConverter implements AuthenticationConverter {

    @Nullable
    @Override
    public Authentication convert(HttpServletRequest request) {
        MultiValueMap<String, String> parameters = formParameters(request);

        String grantType = parameters.getFirst(OAuth2ParameterNames.GRANT_TYPE);
        if (!AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(grantType)) {
            return null;
        }

        // Réservé aux clients publics : rejeter tout secret ou assertion (laissé aux
        // converters confidentiels par défaut).
        if (StringUtils.hasText(request.getHeader("Authorization"))
                || StringUtils.hasText(parameters.getFirst(OAuth2ParameterNames.CLIENT_SECRET))
                || StringUtils.hasText(parameters.getFirst(OAuth2ParameterNames.CLIENT_ASSERTION))) {
            return null;
        }

        String clientId = parameters.getFirst(OAuth2ParameterNames.CLIENT_ID);
        if (!StringUtils.hasText(clientId) || parameters.get(OAuth2ParameterNames.CLIENT_ID).size() != 1) {
            return null;
        }

        Map<String, Object> additionalParameters = new HashMap<>();
        parameters.forEach((key, value) -> {
            if (!OAuth2ParameterNames.CLIENT_ID.equals(key)) {
                additionalParameters.put(key, (value.size() == 1) ? value.get(0) : value.toArray(new String[0]));
            }
        });

        return new OAuth2ClientAuthenticationToken(clientId, ClientAuthenticationMethod.NONE, null,
                additionalParameters);
    }

    private static MultiValueMap<String, String> formParameters(HttpServletRequest request) {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        request.getParameterMap().forEach((key, values) -> parameters.put(key, Arrays.asList(values)));
        return parameters;
    }
}
