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

/**
 * Vérifie un ID token Facebook émis par Limited Login (SDK natif iOS).
 *
 * <p>La discovery de Limited Login ({@code https://limited.facebook.com/.well-known/
 * openid-configuration/}) annonce l'issuer {@code https://www.facebook.com} et le JWKS
 * {@code https://www.facebook.com/.well-known/oauth/openid/jwks/}. La validation est
 * locale : signature, issuer, audience = App ID Facebook, expiration, nonce et email.</p>
 */
@Component
public class FacebookIdTokenVerifier {

    private static final String FACEBOOK_JWK_SET_URI =
            "https://www.facebook.com/.well-known/oauth/openid/jwks/";
    private static final String FACEBOOK_ISSUER = "https://www.facebook.com";

    private final String appId;
    private final JwtDecoder decoder;

    @Autowired
    public FacebookIdTokenVerifier(AppProperties properties) {
        this(
                properties.auth().mobile().facebookAppId(),
                buildDecoder(properties.auth().mobile().facebookAppId())
        );
    }

    FacebookIdTokenVerifier(String appId, JwtDecoder decoder) {
        this.appId = appId;
        this.decoder = decoder;
    }

    /**
     * @throws FacebookAuthProblems.InvalidFacebookTokenProblem si le token est invalide
     * @throws FacebookAuthProblems.FacebookEmailMissingProblem si Facebook n'a pas fourni d'email
     * @throws FacebookAuthProblems.FacebookUnavailableProblem si Facebook est injoignable
     */
    public FacebookIdentity verify(String idToken, String expectedNonce) {
        if (!StringUtils.hasText(appId)) {
            throw new FacebookAuthProblems.FacebookUnavailableProblem(
                    new IllegalStateException("Facebook App ID is not configured"));
        }
        if (!StringUtils.hasText(expectedNonce)) {
            throw new FacebookAuthProblems.InvalidFacebookTokenProblem(
                    new IllegalArgumentException("Missing nonce"));
        }

        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException exception) {
            if (isFacebookUnavailable(exception)) {
                throw new FacebookAuthProblems.FacebookUnavailableProblem(exception);
            }
            throw new FacebookAuthProblems.InvalidFacebookTokenProblem(exception);
        } catch (RestClientException exception) {
            throw new FacebookAuthProblems.FacebookUnavailableProblem(exception);
        }

        if (!StringUtils.hasText(jwt.getSubject())) {
            throw new FacebookAuthProblems.InvalidFacebookTokenProblem();
        }

        String nonce = jwt.getClaimAsString("nonce");
        if (!expectedNonce.equals(nonce)) {
            throw new FacebookAuthProblems.InvalidFacebookTokenProblem(
                    new IllegalArgumentException("Nonce mismatch"));
        }

        String email = jwt.getClaimAsString("email");
        if (!StringUtils.hasText(email)) {
            throw new FacebookAuthProblems.FacebookEmailMissingProblem();
        }

        return new FacebookIdentity(
                jwt.getSubject(),
                email.trim().toLowerCase(Locale.ROOT)
        );
    }

    private static JwtDecoder buildDecoder(String appId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(FACEBOOK_JWK_SET_URI).build();
        decoder.setJwtValidator(validator(appId));
        return decoder;
    }

    /**
     * Validateurs partagés avec les tests : timestamp + issuer Facebook + audience.
     */
    static OAuth2TokenValidator<Jwt> validator(String appId) {
        OAuth2TokenValidator<Jwt> issuer = new JwtClaimValidator<Object>(
                JwtClaimNames.ISS,
                value -> value != null && FACEBOOK_ISSUER.equals(value.toString())
        );
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<Object>(
                JwtClaimNames.AUD,
                value -> isExpectedAudience(value, appId)
        );
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                issuer,
                audience
        );
    }

    private static boolean isExpectedAudience(Object value, String appId) {
        if (value instanceof String audience) {
            return audience.equals(appId);
        }
        return value instanceof List<?> audiences && audiences.contains(appId);
    }

    private static boolean isFacebookUnavailable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RestClientException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}
