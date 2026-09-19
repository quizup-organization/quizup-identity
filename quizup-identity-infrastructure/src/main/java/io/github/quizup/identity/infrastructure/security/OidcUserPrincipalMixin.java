package io.github.quizup.identity.infrastructure.security;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

import java.util.Map;

/**
 * Mixin Jackson du {@link OidcUserPrincipal} pour la (dé)sérialisation des
 * {@code OAuth2Authorization} persistées en JDBC.
 * <p>
 * La (dé)sérialisation est basée sur les <b>champs</b> uniquement
 * ({@code getterVisibility = NONE}) : les getters dérivés hérités de
 * {@code OidcUser}/{@code IdTokenClaimAccessor} (audience, issuer, nonce,
 * expiresAt, …) ne sont donc jamais introspectés, ce qui évite les erreurs
 * « setterless property » en default typing.
 */
@JsonAutoDetect(
        fieldVisibility = Visibility.ANY,
        getterVisibility = Visibility.NONE,
        isGetterVisibility = Visibility.NONE,
        setterVisibility = Visibility.NONE)
@JsonIgnoreProperties(value = {"attributes", "authorities", "claims"}, ignoreUnknown = true)
abstract class OidcUserPrincipalMixin {

    @JsonCreator
    OidcUserPrincipalMixin(
            @JsonProperty("userId") String userId,
            @JsonProperty("email") String email,
            @JsonProperty("idToken") OidcIdToken idToken,
            @JsonProperty("userInfo") OidcUserInfo userInfo,
            @JsonProperty("attributes") Map<String, Object> attributes) {
    }
}
