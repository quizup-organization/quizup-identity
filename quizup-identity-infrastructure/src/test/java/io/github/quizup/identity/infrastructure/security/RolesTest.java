package io.github.quizup.identity.infrastructure.security;

import io.github.quizup.identity.infrastructure.properties.AppProperties;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolesTest {

    @Test
    void systemUserGetsUserAndAdminRoles() {
        Roles roles = roles("");

        List<String> authorities = authoritiesOf(roles, QuizUpConstants.SYSTEM_USER_ID, "system@quizup.dev");

        assertTrue(authorities.contains(Roles.ROLE_USER));
        assertTrue(authorities.contains(Roles.ROLE_ADMIN));
    }

    @Test
    void allowlistedEmailGetsAdminRole() {
        Roles roles = roles("c.nadjim@gmail.com");

        List<String> authorities = authoritiesOf(roles, "random-id", "c.nadjim@gmail.com");

        assertTrue(authorities.contains(Roles.ROLE_ADMIN));
    }

    @Test
    void allowlistedEmailIsCaseInsensitive() {
        Roles roles = roles(" C.Nadjim@Gmail.com ");

        List<String> authorities = authoritiesOf(roles, "random-id", "c.nadjim@gmail.com");

        assertTrue(authorities.contains(Roles.ROLE_ADMIN));
    }

    @Test
    void regularUserGetsOnlyUserRole() {
        Roles roles = roles("c.nadjim@gmail.com");

        List<String> authorities = authoritiesOf(roles, "c0ffee00-0000-0000-0000-000000000000", "other@quizup.dev");

        assertEquals(List.of(Roles.ROLE_USER), authorities);
    }

    private Roles roles(String adminEmails) {
        AppProperties properties = new AppProperties(
                new AppProperties.AuthorizationServer("http://localhost:8085", "", adminEmails),
                new AppProperties.Security(
                        "http://localhost:5173/login",
                        new AppProperties.Security.OAuth2("success", "failure"),
                        new AppProperties.Security.Cors(List.of("http://localhost:5173")),
                        new AppProperties.Security.Session("AUTH_TX", "Lax", false, "/")),
                new AppProperties.Mail("", "from", "base-url"),
                new AppProperties.Jwk("", false),
                new AppProperties.Auth(""),
                new AppProperties.SeedData(false));
        return new Roles(properties);
    }

    private List<String> authoritiesOf(Roles roles, String userId, String email) {
        return roles.forUser(userId, email).stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
