package io.github.quizup.identity.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Vérifie que les principals QuizUp passent l'allowlist Jackson du serveur
 * d'autorisation (grâce aux mixins) et font un aller-retour (dé)sérialisation,
 * y compris avec des claims d'id_token <b>réalistes</b> (aud, iss, nonce, azp…)
 * qui déclenchaient l'erreur « setterless property ».
 */
class OAuth2AuthorizationObjectMapperTest {

    private final ObjectMapper objectMapper = OAuth2AuthorizationObjectMapper.create();

    @Test
    void userPrincipal_roundTrips() throws Exception {
        UserPrincipal principal = new UserPrincipal("user-1", "user@quizup.dev", authorities());

        String json = objectMapper.writeValueAsString(principal);

        Object restored = objectMapper.readValue(json, Object.class);
        UserPrincipal user = assertInstanceOf(UserPrincipal.class, restored);
        assertEquals("user-1", user.getUserId());
        assertEquals("user@quizup.dev", user.getEmail());
    }

    @Test
    void oidcUserPrincipal_roundTrips_withRealisticClaims() throws Exception {
        OidcUserPrincipal principal = new OidcUserPrincipal(
                "user-2", "social@quizup.dev", idToken("google-sub-2", "social@quizup.dev"), userInfo("user-2", "social@quizup.dev"), Map.of(), authorities());

        Object restored = roundTrip(principal);

        OidcUserPrincipal user = assertInstanceOf(OidcUserPrincipal.class, restored);
        assertEquals("user-2", user.getUserId());
        assertEquals("social@quizup.dev", user.getEmail());
    }

    @Test
    void usernamePasswordAuthentication_roundTrips() throws Exception {
        UserPrincipal principal = new UserPrincipal("user-3", "local@quizup.dev", authorities());
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());

        Object restored = roundTrip(authentication);

        UsernamePasswordAuthenticationToken token =
                assertInstanceOf(UsernamePasswordAuthenticationToken.class, restored);
        UserPrincipal user = assertInstanceOf(UserPrincipal.class, token.getPrincipal());
        assertEquals("user-3", user.getUserId());
        assertEquals("local@quizup.dev", user.getEmail());
    }

    @Test
    void oauth2AuthenticationToken_roundTrips_withRealisticClaims() throws Exception {
        OidcUserPrincipal principal = new OidcUserPrincipal(
                "user-4", "social2@quizup.dev", idToken("google-sub-4", "social2@quizup.dev"), userInfo("user-4", "social2@quizup.dev"), Map.of(), authorities());
        OAuth2AuthenticationToken authentication =
                new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");

        Object restored = roundTrip(authentication);

        OAuth2AuthenticationToken token = assertInstanceOf(OAuth2AuthenticationToken.class, restored);
        OidcUserPrincipal user = assertInstanceOf(OidcUserPrincipal.class, token.getPrincipal());
        assertEquals("user-4", user.getUserId());
    }

    private java.util.List<org.springframework.security.core.GrantedAuthority> authorities() {
        return java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"));
    }

    private OidcIdToken idToken(String subject, String email) {
        Instant issuedAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant expiresAt = Instant.parse("2026-01-01T01:00:00Z");

        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", "https://accounts.google.com");
        claims.put("sub", subject);
        claims.put("aud", "270229696016-quizup.apps.googleusercontent.com");
        claims.put("azp", "270229696016-quizup.apps.googleusercontent.com");
        claims.put("nonce", "nonce-abc");
        claims.put("auth_time", issuedAt);
        claims.put("at_hash", "at-hash");
        claims.put("email", email);
        claims.put("email_verified", true);

        return new OidcIdToken("id-token-" + subject, issuedAt, expiresAt, claims);
    }

    private OidcUserInfo userInfo(String userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        claims.put("user_id", userId);
        claims.put("email", email);
        claims.put("email_verified", true);
        claims.put("name", "QuizUp User");

        return new OidcUserInfo(claims);
    }

    private Object roundTrip(Object value) throws Exception {
        String json = objectMapper.writeValueAsString(value);
        return objectMapper.readValue(json, Object.class);
    }
}
