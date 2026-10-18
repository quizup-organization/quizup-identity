package io.github.quizup.identity.infrastructure.security;

import io.github.quizup.identity.infrastructure.properties.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Vérifie un ID token Google émis par le SDK natif mobile.
 *
 * <p>Validation locale (JWKS Google, sans appel au endpoint tokeninfo) : signature,
 * issuer Google (deux formes acceptées), audience = web client ID, expiration, email
 * vérifié. L'audience correspond au {@code webClientId} configuré côté SDK : c'est le
 * client OAuth2 « Web » Google, pas le client Android.</p>
 */
@Component
public class GoogleIdTokenVerifier {

    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of(
            "https://accounts.google.com",
            "accounts.google.com"
    );

    private final String clientId;
    private final JwtDecoder decoder;

    @Autowired
    public GoogleIdTokenVerifier(AppProperties properties) {
        this(
                properties.auth().mobile().googleClientId(),
                buildDecoder(properties.auth().mobile().googleClientId())
        );
    }

    GoogleIdTokenVerifier(String clientId, JwtDecoder decoder) {
        this.clientId = clientId;
        this.decoder = decoder;
    }

    /**
     * @throws GoogleAuthProblems.InvalidGoogleTokenProblem si le token est invalide
     * @throws GoogleAuthProblems.GoogleUnavailableProblem si Google est injoignable
     */
    public GoogleIdentity verify(String idToken) {
        if (!StringUtils.hasText(clientId)) {
            throw new GoogleAuthProblems.GoogleUnavailableProblem(
                    new IllegalStateException("Google client ID is not configured"));
        }

        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException exception) {
            if (isGoogleUnavailable(exception)) {
                throw new GoogleAuthProblems.GoogleUnavailableProblem(exception);
            }
            throw new GoogleAuthProblems.InvalidGoogleTokenProblem(exception);
        } catch (RestClientException exception) {
            throw new GoogleAuthProblems.GoogleUnavailableProblem(exception);
        }

        String email = jwt.getClaimAsString("email");
        if (!StringUtils.hasText(email) || !StringUtils.hasText(jwt.getSubject())) {
            throw new GoogleAuthProblems.InvalidGoogleTokenProblem();
        }

        return new GoogleIdentity(jwt.getSubject(), email.trim().toLowerCase(Locale.ROOT));
    }

    private static JwtDecoder buildDecoder(String clientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();
        decoder.setJwtValidator(validator(clientId));
        return decoder;
    }

    /**
     * Validateurs partagés avec les tests : timestamp + issuer Google + audience + email vérifié.
     */
    static OAuth2TokenValidator<Jwt> validator(String clientId) {
        OAuth2TokenValidator<Jwt> issuer = new JwtClaimValidator<Object>(
                JwtClaimNames.ISS,
                value -> value != null && GOOGLE_ISSUERS.contains(value.toString())
        );
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<Object>(
                JwtClaimNames.AUD,
                value -> isExpectedAudience(value, clientId)
        );
        OAuth2TokenValidator<Jwt> emailVerified = new JwtClaimValidator<Object>(
                "email_verified",
                GoogleIdTokenVerifier::isEmailVerified
        );
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                issuer,
                audience,
                emailVerified
        );
    }

    private static boolean isEmailVerified(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static boolean isExpectedAudience(Object value, String clientId) {
        if (value instanceof String audience) {
            return audience.equals(clientId);
        }
        return value instanceof List<?> audiences && audiences.contains(clientId);
    }

    private static boolean isGoogleUnavailable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RestClientException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /** Identité Google vérifiée (subject Google + email). */
    public record GoogleIdentity(String subject, String email) {
    }
}
