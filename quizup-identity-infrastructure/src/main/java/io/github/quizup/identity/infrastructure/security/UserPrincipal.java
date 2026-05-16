package io.github.quizup.identity.infrastructure.security;

import io.github.quizup.common.domain.model.security.QuizUpPrincipal;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Principal utilisateur implémentant à la fois UserDetails et OAuth2User
 */
public class UserPrincipal implements UserDetails, OAuth2User, QuizUpPrincipal {

    @Getter
    private final String userId;

    @Getter
    private final String email;
    private final String password;
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(String userId, String email, String password) {
        this(userId, email, password, Map.of());
    }

    public UserPrincipal(String userId, String email, String password, Map<String, Object> attributes) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.attributes = attributes;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
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
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return userId;
    }
}
