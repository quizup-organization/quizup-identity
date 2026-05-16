package io.github.quizup.identity.infrastructure.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import io.github.quizup.common.domain.model.security.QuizUpPrincipal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Principal utilisateur pour OIDC (OpenID Connect)
 * Utilisé pour les logins Google, etc.
 */
public class OidcUserPrincipal implements OidcUser, QuizUpPrincipal {

    @Getter
    private final String userId;

    @Getter
    private final String email;

    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;

    public OidcUserPrincipal(String userId, String email, OidcIdToken idToken, OidcUserInfo userInfo, Map<String, Object> attributes) {
        this.userId = userId;
        this.email = email;
        this.idToken = idToken;
        this.userInfo = userInfo;
        this.attributes = attributes;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public Map<String, Object> getClaims() {
        return idToken.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
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
    public String getName() {
        return userId;
    }
}
