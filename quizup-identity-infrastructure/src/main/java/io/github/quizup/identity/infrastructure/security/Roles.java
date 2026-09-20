package io.github.quizup.identity.infrastructure.security;

import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rôles applicatifs émis dans le claim JWT {@code roles}.
 * <p>
 * Le compte système ({@link QuizUpConstants#SYSTEM_USER_ID}) et toute adresse de la
 * liste blanche {@code app.authorization-server.admin-emails} reçoivent {@link #ROLE_ADMIN}
 * en plus de {@link #ROLE_USER}. Ce claim est consommé en aval (ex. Grafana via
 * {@code role_attribute_path}).
 */
@Component
public class Roles {

    static final String ROLE_USER = "ROLE_USER";
    static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final Set<String> adminEmails;

    public Roles(@Value("${app.authorization-server.admin-emails:}") String adminEmails) {
        this.adminEmails = Arrays.stream(adminEmails.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public List<GrantedAuthority> forUser(String userId, String email) {
        if (QuizUpConstants.SYSTEM_USER_ID.equals(userId) || isAdminEmail(email)) {
            return List.of(new SimpleGrantedAuthority(ROLE_USER), new SimpleGrantedAuthority(ROLE_ADMIN));
        }
        return List.of(new SimpleGrantedAuthority(ROLE_USER));
    }

    private boolean isAdminEmail(String email) {
        return email != null && adminEmails.contains(email.trim().toLowerCase(Locale.ROOT));
    }
}
