package io.github.quizup.identity.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Propriétés applicatives d'identity (`app.*`), centralisées en records immuables.
 *
 * <p>Toutes les valeurs ont un défaut local ; les environnements les surchargent via les
 * placeholders des `application-*.yml`. Ce bean remplace les `@Value` dispersés dans
 * {@code AuthorizationServerConfig}, {@code SessionConfig}, {@code JwkConfig},
 * {@code ResendEmailAdapter}, {@code JwtTokenCustomizer}, {@code Roles},
 * {@code PasswordlessAuthService} et {@code DataSeeder}.
 */
@ConfigurationProperties("app")
public record AppProperties(
        @DefaultValue AuthorizationServer authorizationServer,
        @DefaultValue Security security,
        @DefaultValue Mail mail,
        @DefaultValue Jwk jwk,
        @DefaultValue Auth auth,
        @DefaultValue SeedData seedData) {

    public record AuthorizationServer(
            @DefaultValue("http://localhost:8085") String issuer,
            @DefaultValue("") String audience,
            @DefaultValue("") String adminEmails) {
    }

    public record Security(
            @DefaultValue("http://localhost:5173/login") String loginPageUri,
            @DefaultValue OAuth2 oauth2,
            @DefaultValue Cors cors,
            @DefaultValue Session session) {

        public record OAuth2(
                @DefaultValue("http://localhost:5173/login?social=success") String successRedirectUri,
                @DefaultValue("http://localhost:5173/login?error") String failureRedirectUri) {
        }

        public record Cors(
                @DefaultValue("http://localhost:5173") List<String> allowedOrigins) {
        }

        public record Session(
                @DefaultValue("AUTH_TX") String cookieName,
                @DefaultValue("Lax") String cookieSameSite,
                @DefaultValue("false") boolean cookieSecure,
                @DefaultValue("/") String cookiePath) {
        }
    }

    public record Mail(
            @DefaultValue("") String apiKey,
            @DefaultValue("QuizUp <no-reply@quizup.cnadjim.fr>") String from,
            @DefaultValue("https://api.resend.com") String baseUrl) {
    }

    public record Jwk(
            @DefaultValue("") String jwkSet,
            @DefaultValue("false") boolean generateIfMissing) {
    }

    public record Auth(
            @DefaultValue("") String devFixedCode) {
    }

    public record SeedData(
            @DefaultValue("false") boolean enabled) {
    }
}
