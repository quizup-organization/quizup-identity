package io.github.quizup.identity.infrastructure.security;

import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolesTest {

    @Test
    void systemUserGetsUserAndAdminRoles() {
        Roles roles = new Roles("");

        List<String> authorities = authoritiesOf(roles, QuizUpConstants.SYSTEM_USER_ID, "system@quizup.dev");

        assertTrue(authorities.contains(Roles.ROLE_USER));
        assertTrue(authorities.contains(Roles.ROLE_ADMIN));
    }

    @Test
    void allowlistedEmailGetsAdminRole() {
        Roles roles = new Roles("c.nadjim@gmail.com");

        List<String> authorities = authoritiesOf(roles, "random-id", "c.nadjim@gmail.com");

        assertTrue(authorities.contains(Roles.ROLE_ADMIN));
    }

    @Test
    void allowlistedEmailIsCaseInsensitive() {
        Roles roles = new Roles(" C.Nadjim@Gmail.com ");

        List<String> authorities = authoritiesOf(roles, "random-id", "c.nadjim@gmail.com");

        assertTrue(authorities.contains(Roles.ROLE_ADMIN));
    }

    @Test
    void regularUserGetsOnlyUserRole() {
        Roles roles = new Roles("c.nadjim@gmail.com");

        List<String> authorities = authoritiesOf(roles, "c0ffee00-0000-0000-0000-000000000000", "other@quizup.dev");

        assertEquals(List.of(Roles.ROLE_USER), authorities);
    }

    private List<String> authoritiesOf(Roles roles, String userId, String email) {
        return roles.forUser(userId, email).stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
