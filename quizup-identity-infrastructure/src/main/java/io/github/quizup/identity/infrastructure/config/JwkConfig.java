package io.github.quizup.identity.infrastructure.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * Source de clés de signature JWT partagée par toutes les instances.
 * <p>
 * En production, la JWK Set est fournie par le secret Kubernetes via
 * {@code app.jwk.jwk-set} (env {@code QUIZUP_IDENTITY_JWK}) : les instances ne
 * peuvent donc pas signer avec des clés divergentes.
 * <p>
 * La génération éphémère est contrôlée par {@code app.jwk.generate-if-missing}
 * (true uniquement en profil {@code local}) : sans cette activation explicite,
 * l'absence de clé fait échouer le démarrage.
 */
@Configuration
public class JwkConfig {

    private static final Logger logger = LoggerFactory.getLogger(JwkConfig.class);

    @Bean
    public JWKSource<SecurityContext> jwkSource(
            @Value("${app.jwk.jwk-set:}") String jwkSet,
            @Value("${app.jwk.generate-if-missing:false}") boolean generateIfMissing) {

        if (jwkSet != null && !jwkSet.isBlank()) {
            return fromConfiguredJwkSet(jwkSet);
        }

        if (generateIfMissing) {
            logger.warn("app.jwk.jwk-set is not set: generating an EPHEMERAL RSA key pair "
                    + "(app.jwk.generate-if-missing=true). Do not use this in a multi-instance deployment.");
            return new ImmutableJWKSet<>(new JWKSet(generateRsaKey()));
        }

        throw new IllegalStateException(
                "app.jwk.jwk-set (env QUIZUP_IDENTITY_JWK) is required: signing keys must be shared across instances");
    }

    private JWKSource<SecurityContext> fromConfiguredJwkSet(String jwkSet) {
        try {
            return new ImmutableJWKSet<>(JWKSet.parse(jwkSet));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse app.jwk.jwk-set as a JWK Set", ex);
        }
    }

    private static RSAKey generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair", ex);
        }
    }
}
