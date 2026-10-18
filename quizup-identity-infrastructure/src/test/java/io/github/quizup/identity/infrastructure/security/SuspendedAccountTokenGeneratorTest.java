package io.github.quizup.identity.infrastructure.security;

import io.github.quizup.identity.domain.model.AccountStatus;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.model.UserSuspension;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import io.github.quizup.microservice.core.domain.model.security.QuizUpPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SuspendedAccountTokenGeneratorTest {

    private final OAuth2TokenGenerator<OAuth2Token> delegate = mock(OAuth2TokenGenerator.class);
    private final UserRepositoryPort repository = mock(UserRepositoryPort.class);
    private final OAuth2TokenContext context = mock(OAuth2TokenContext.class);
    private final Authentication authentication = mock(Authentication.class);
    private final QuizUpPrincipal principal = mock(QuizUpPrincipal.class);
    private final SuspendedAccountTokenGenerator generator =
            new SuspendedAccountTokenGenerator(delegate, repository);

    @Test
    void suspendedPrincipal_cannotObtainToken() {
        when(context.getPrincipal()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(principal.getUserId()).thenReturn("user-1");
        when(repository.findById("user-1")).thenReturn(Optional.of(new User(
                "user-1", "user@quizup.dev", Set.of(), Instant.now(),
                AccountStatus.SUSPENDED,
                new UserSuspension("s1", io.github.quizup.identity.domain.model.UserSuspensionReason.SPAM,
                        null, "admin-1", Instant.now(), Instant.now().plusSeconds(3600),
                        null, null, null, null))));

        assertThatThrownBy(() -> generator.generate(context))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void activePrincipal_delegates() {
        when(context.getPrincipal()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(principal.getUserId()).thenReturn("user-1");
        when(repository.findById("user-1")).thenReturn(Optional.of(new User(
                "user-1", "user@quizup.dev", Set.of(), Instant.now())));
        OAuth2Token token = mock(OAuth2Token.class);
        when(delegate.generate(any())).thenReturn(token);

        org.assertj.core.api.Assertions.assertThat(generator.generate(context)).isSameAs(token);
    }

    @Test
    void clientCredentialsPrincipal_skipsUserCheck() {
        when(context.getPrincipal()).thenReturn(null);
        OAuth2Token token = mock(OAuth2Token.class);
        when(delegate.generate(any())).thenReturn(token);

        org.assertj.core.api.Assertions.assertThat(generator.generate(context)).isSameAs(token);
    }
}
