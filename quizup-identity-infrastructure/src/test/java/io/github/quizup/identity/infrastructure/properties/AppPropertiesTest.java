package io.github.quizup.identity.infrastructure.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie que {@link AppProperties} se lie sans aucun `app.*` déclaré (défauts complets)
 * et que les propriétés explicites surchargent bien les défauts.
 */
class AppPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class);

    @Test
    void bindsFullDefaultsWhenNothingConfigured() {
        runner.run(context -> {
            AppProperties properties = context.getBean(AppProperties.class);

            assertThat(properties.authorizationServer().audience()).isEmpty();
            assertThat(properties.authorizationServer().adminEmails()).isEmpty();

            assertThat(properties.security().loginPageUri()).isEqualTo("http://localhost:5173/login");
            assertThat(properties.security().oauth2().successRedirectUri())
                    .isEqualTo("http://localhost:5173/login?social=success");
            assertThat(properties.security().oauth2().failureRedirectUri())
                    .isEqualTo("http://localhost:5173/login?error");
            assertThat(properties.security().cors().allowedOrigins())
                    .containsExactly("http://localhost:5173");
            assertThat(properties.security().session().cookieName()).isEqualTo("AUTH_TX");
            assertThat(properties.security().session().cookieSameSite()).isEqualTo("Lax");
            assertThat(properties.security().session().cookieSecure()).isFalse();
            assertThat(properties.security().session().cookiePath()).isEqualTo("/");

            assertThat(properties.mail().apiKey()).isEmpty();
            assertThat(properties.mail().from()).isEqualTo("QuizUp <no-reply@quizup.cnadjim.fr>");
            assertThat(properties.mail().baseUrl()).isEqualTo("https://api.resend.com");

            assertThat(properties.jwk().jwkSet()).isEmpty();
            assertThat(properties.jwk().generateIfMissing()).isFalse();
            assertThat(properties.auth().devFixedCode()).isEmpty();
            assertThat(properties.seedData().enabled()).isFalse();
        });
    }

    @Test
    void explicitValuesOverrideDefaults() {
        runner.withPropertyValues(
                        "app.authorization-server.admin-emails=admin@quizup.local",
                        "app.security.session.cookie-secure=true",
                        "app.security.cors.allowed-origins=https://app.quizup.local,https://app2.quizup.local",
                        "app.seed-data.enabled=true")
                .run(context -> {
                    AppProperties properties = context.getBean(AppProperties.class);

                    assertThat(properties.authorizationServer().adminEmails()).isEqualTo("admin@quizup.local");
                    assertThat(properties.security().session().cookieSecure()).isTrue();
                    assertThat(properties.security().cors().allowedOrigins())
                            .containsExactly("https://app.quizup.local", "https://app2.quizup.local");
                    assertThat(properties.seedData().enabled()).isTrue();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AppProperties.class)
    static class Config {
    }
}
