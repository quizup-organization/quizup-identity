package io.github.quizup.identity.infrastructure.security;

import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolesTest {

    @Test
    void adminUserGetsUserAndAdminRoles() {
        List<String> authorities = Roles.forUser(QuizUpConstants.ADMIN_USER_ID).stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertTrue(authorities.contains(Roles.ROLE_USER));
        assertTrue(authorities.contains(Roles.ROLE_ADMIN));
    }

    @Test
    void regularUserGetsOnlyUserRole() {
        List<String> authorities = Roles.forUser("c0ffee00-0000-0000-0000-000000000000").stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertEquals(List.of(Roles.ROLE_USER), authorities);
    }
}
