package io.github.quizup.identity.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie que la registration Facebook se construit même sans App Secret (démarrage
 * local) et que les URLs Graph versionnées sont bien appliquées.
 */
class FacebookOAuth2ClientRegistrationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OAuth2ClientAutoConfiguration.class))
            .withPropertyValues(
                    "spring.security.oauth2.client.registration.facebook.provider=facebook",
                    "spring.security.oauth2.client.registration.facebook.client-id=2122240041993058",
                    "spring.security.oauth2.client.registration.facebook.client-secret=",
                    "spring.security.oauth2.client.registration.facebook.client-authentication-method=client_secret_post",
                    "spring.security.oauth2.client.registration.facebook.authorization-grant-type=authorization_code",
                    "spring.security.oauth2.client.registration.facebook.scope=public_profile,email",
                    "spring.security.oauth2.client.provider.facebook.authorization-uri=https://www.facebook.com/v26.0/dialog/oauth",
                    "spring.security.oauth2.client.provider.facebook.token-uri=https://graph.facebook.com/v26.0/oauth/access_token",
                    "spring.security.oauth2.client.provider.facebook.user-info-uri=https://graph.facebook.com/v26.0/me?fields=id,name,email,picture",
                    "spring.security.oauth2.client.provider.facebook.user-name-attribute=id"
            );

    @Test
    void buildsFacebookRegistrationWithGraphV26Urls() {
        runner.run(context -> {
            ClientRegistrationRepository repository =
                    context.getBean(ClientRegistrationRepository.class);
            ClientRegistration registration = repository.findByRegistrationId("facebook");

            assertThat(registration).isNotNull();
            assertThat(registration.getClientId()).isEqualTo("2122240041993058");
            assertThat(registration.getScopes()).containsExactlyInAnyOrder("public_profile", "email");
            assertThat(registration.getProviderDetails().getAuthorizationUri())
                    .isEqualTo("https://www.facebook.com/v26.0/dialog/oauth");
            assertThat(registration.getProviderDetails().getTokenUri())
                    .isEqualTo("https://graph.facebook.com/v26.0/oauth/access_token");
            assertThat(registration.getProviderDetails().getUserInfoEndpoint().getUri())
                    .isEqualTo("https://graph.facebook.com/v26.0/me?fields=id,name,email,picture");
            assertThat(registration.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName())
                    .isEqualTo("id");
        });
    }
}
