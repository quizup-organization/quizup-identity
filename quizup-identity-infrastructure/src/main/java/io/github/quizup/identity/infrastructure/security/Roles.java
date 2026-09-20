package io.github.quizup.identity.infrastructure.security;

import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Rôles applicatifs émis dans le claim JWT {@code roles}.
 * <p>
 * L'utilisateur administrateur système ({@link QuizUpConstants#ADMIN_USER_ID}) reçoit
 * {@link #ROLE_ADMIN} en plus de {@link #ROLE_USER}. Tous les autres utilisateurs ne reçoivent
 * que {@link #ROLE_USER}. Ce claim est consommé en aval (ex. Grafana via {@code role_attribute_path}).
 */
final class Roles {

    static final String ROLE_USER = "ROLE_USER";
    static final String ROLE_ADMIN = "ROLE_ADMIN";

    private Roles() {
    }

    static List<GrantedAuthority> forUser(String userId) {
        if (QuizUpConstants.ADMIN_USER_ID.equals(userId)) {
            return List.of(new SimpleGrantedAuthority(ROLE_USER), new SimpleGrantedAuthority(ROLE_ADMIN));
        }
        return List.of(new SimpleGrantedAuthority(ROLE_USER));
    }
}
