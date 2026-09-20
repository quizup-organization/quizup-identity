package io.github.quizup.identity.infrastructure.security;

import io.github.quizup.microservice.core.domain.model.security.QuizUpPrincipal;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Principal utilisateur implémentant à la fois UserDetails et OAuth2User.
 * Serializable : le SecurityContext est persisté dans la session Spring Session JDBC.
 *
 * <p>Passwordless : aucun mot de passe n'est porté par le principal.</p>
 */
public class UserPrincipal implements UserDetails, OAuth2User, QuizUpPrincipal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    private final String userId;

    @Getter
    private final String email;

    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(String userId, String email, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.attributes = Map.of();
        this.authorities = authorities != null ? authorities : java.util.List.of();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getName() {
        return userId;
    }
}
