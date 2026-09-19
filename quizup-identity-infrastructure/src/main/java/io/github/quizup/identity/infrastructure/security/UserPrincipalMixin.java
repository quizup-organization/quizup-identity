package io.github.quizup.identity.infrastructure.security;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mixin Jackson du {@link UserPrincipal} pour la (dé)sérialisation des
 * {@code OAuth2Authorization} persistées en JDBC.
 * <p>
 * (Dé)sérialisation basée sur les <b>champs</b> uniquement
 * ({@code getterVisibility = NONE}) afin de ne jamais introspecter les getters
 * hérités de {@code UserDetails}/{@code OAuth2User}. Le hash du mot de passe
 * n'est jamais sérialisé ({@code WRITE_ONLY}).
 */
@JsonAutoDetect(
        fieldVisibility = Visibility.ANY,
        getterVisibility = Visibility.NONE,
        isGetterVisibility = Visibility.NONE,
        setterVisibility = Visibility.NONE)
@JsonIgnoreProperties(value = {"attributes", "authorities", "name", "username"}, ignoreUnknown = true)
abstract class UserPrincipalMixin {

    @JsonCreator
    UserPrincipalMixin(
            @JsonProperty(value = "userId") String userId,
            @JsonProperty(value = "email") String email,
            @JsonProperty(value = "password", access = JsonProperty.Access.WRITE_ONLY) String password) {
    }
}
