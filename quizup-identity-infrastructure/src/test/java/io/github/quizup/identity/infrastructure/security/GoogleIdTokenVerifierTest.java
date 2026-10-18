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

class GoogleIdTokenVerifierTest {

    private static final String CLIENT_ID = "quizup-web-client";
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    private static KeyPair keyPair;
    private static JwtEncoder encoder;
    private static GoogleIdTokenVerifier verifier;

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
        decoder.setJwtValidator(GoogleIdTokenVerifier.validator(CLIENT_ID));
        verifier = new GoogleIdTokenVerifier(CLIENT_ID, decoder);
    }

    @Test
    void verifiesValidTokenAndNormalizesEmail() {
        String token = token(GOOGLE_ISSUER, CLIENT_ID, "  Player@Example.COM ", true,
                Instant.now(), Instant.now().plusSeconds(300));

        GoogleIdTokenVerifier.GoogleIdentity identity = verifier.verify(token);

        assertThat(identity.subject()).isEqualTo("google-sub-1");
        assertThat(identity.email()).isEqualTo("player@example.com");
    }

    @Test
    void rejectsWrongAudience() {
        String token = token(GOOGLE_ISSUER, "another-client", "player@example.com", true,
                Instant.now(), Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(GoogleAuthProblems.InvalidGoogleTokenProblem.class);
    }

    @Test
    void rejectsWrongIssuer() {
        String token = token("https://evil.example", CLIENT_ID, "player@example.com", true,
                Instant.now(), Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(GoogleAuthProblems.InvalidGoogleTokenProblem.class);
    }

    @Test
    void rejectsUnverifiedEmail() {
        String token = token(GOOGLE_ISSUER, CLIENT_ID, "player@example.com", false,
                Instant.now(), Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(GoogleAuthProblems.InvalidGoogleTokenProblem.class);
    }

    @Test
    void rejectsExpiredToken() {
        String token = token(GOOGLE_ISSUER, CLIENT_ID, "player@example.com", true,
                Instant.now().minusSeconds(600), Instant.now().minusSeconds(300));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(GoogleAuthProblems.InvalidGoogleTokenProblem.class);
    }

    @Test
    void rejectsTokenWithoutEmail() {
        String token = token(GOOGLE_ISSUER, CLIENT_ID, "", true,
                Instant.now(), Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(GoogleAuthProblems.InvalidGoogleTokenProblem.class);
    }

    private static String token(String issuer, String audience, String email, Object emailVerified,
                                Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("google-sub-1")
                .audience(List.of(audience))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("email", email)
                .claim("email_verified", emailVerified)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
