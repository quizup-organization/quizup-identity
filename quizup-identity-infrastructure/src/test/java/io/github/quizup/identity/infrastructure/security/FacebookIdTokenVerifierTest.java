package io.github.quizup.identity.infrastructure.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FacebookIdTokenVerifierTest {

    private static final String APP_ID = "2122240041993058";
    private static final String FACEBOOK_ISSUER = "https://www.facebook.com";
    private static final String NONCE = "nonce-123";

    private static KeyPair keyPair;
    private static JwtEncoder encoder;
    private static FacebookIdTokenVerifier verifier;

    @BeforeAll
    static void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID("test-key")
                .build();
        encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
        decoder.setJwtValidator(FacebookIdTokenVerifier.validator(APP_ID));
        verifier = new FacebookIdTokenVerifier(APP_ID, decoder);
    }

    @Test
    void verifiesValidTokenAndNormalizesEmail() {
        String token = token(FACEBOOK_ISSUER, APP_ID, "  Player@Example.COM ", NONCE,
                Instant.now(), Instant.now().plusSeconds(300));

        FacebookIdentity identity = verifier.verify(token, NONCE);

        assertThat(identity.subject()).isEqualTo("facebook-sub-1");
        assertThat(identity.email()).isEqualTo("player@example.com");
    }

    @Test
    void rejectsWrongAudience() {
        String token = token(FACEBOOK_ISSUER, "another-app", "player@example.com", NONCE,
                Instant.now(), Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> verifier.verify(token, NONCE))
                .isInstanceOf(FacebookAuthProblems.InvalidFacebookTokenProblem.class);
    }

    @Test
    void rejectsWrongIssuer() {
        String token = token("https://evil.example", APP_ID, "player@example.com", NONCE,
                Instant.now(), Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> verifier.verify(token, NONCE))
                .isInstanceOf(FacebookAuthProblems.InvalidFacebookTokenProblem.class);
    }

    @Test
    void rejectsExpiredToken() {
        String token = token(FACEBOOK_ISSUER, APP_ID, "player@example.com", NONCE,
                Instant.now().minusSeconds(600), Instant.now().minusSeconds(300));

        assertThatThrownBy(() -> verifier.verify(token, NONCE))
                .isInstanceOf(FacebookAuthProblems.InvalidFacebookTokenProblem.class);
    }

    @Test
    void rejectsWrongNonce() {
        String token = token(FACEBOOK_ISSUER, APP_ID, "player@example.com", NONCE,
                Instant.now(), Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> verifier.verify(token, "another-nonce"))
                .isInstanceOf(FacebookAuthProblems.InvalidFacebookTokenProblem.class);
    }

    @Test
    void rejectsMissingEmail() {
        String token = token(FACEBOOK_ISSUER, APP_ID, "", NONCE,
                Instant.now(), Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> verifier.verify(token, NONCE))
                .isInstanceOf(FacebookAuthProblems.FacebookEmailMissingProblem.class);
    }

    private static String token(String issuer, String audience, String email, String nonce,
                                Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("facebook-sub-1")
                .audience(List.of(audience))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("email", email)
                .claim("nonce", nonce)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
