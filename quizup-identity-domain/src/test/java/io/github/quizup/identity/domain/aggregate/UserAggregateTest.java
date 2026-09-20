package io.github.quizup.identity.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.identity.domain.command.UserCommand;
import io.github.quizup.identity.domain.event.UserEvent;
import io.github.quizup.identity.domain.exception.UserProblems;
import io.github.quizup.identity.domain.model.SocialProvider;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test Axon in-memory de l'agrégat {@link UserAggregate} via {@link AggregateTestFixture}.
 * <p>
 * 100 % in-memory : event store de l'agrégat en mémoire, aucun Postgres ni Axon Server.
 * Les ports sortant (mocks) sont déclarés comme resources injectables du fixture.
 */
class UserAggregateTest {

    private final AggregateTestFixture<UserAggregate> fixture =
            new AggregateTestFixture<>(UserAggregate.class);

    @Test
    void registerUser_appliesUserRegisteredEventWithoutProvider() {
        UserRepositoryPort readPort = mock(UserRepositoryPort.class);
        when(readPort.existsByEmail(anyString())).thenReturn(false);

        fixture.registerInjectableResource(readPort)
                .givenNoPriorActivity()
                .when(new UserCommand.RegisterUserCommand("user-1", "user@quizup.dev"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        UserEvent.UserRegisteredEvent.class,
                        e -> {
                            UserEvent.UserRegisteredEvent event = (UserEvent.UserRegisteredEvent) e;
                            return event.userId().equals("user-1")
                                    && event.email().equals("user@quizup.dev")
                                    && event.provider() == null
                                    && event.createdAt() != null;
                        }));
    }

    @Test
    void registerWithSocial_appliesUserRegisteredEventWithProvider() {
        UserRepositoryPort readPort = mock(UserRepositoryPort.class);
        when(readPort.existsByEmail(anyString())).thenReturn(false);

        fixture.registerInjectableResource(readPort)
                .givenNoPriorActivity()
                .when(new UserCommand.RegisterUserWithSocialCommand("user-2", "user@quizup.dev", SocialProvider.GOOGLE))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        UserEvent.UserRegisteredEvent.class,
                        e -> {
                            UserEvent.UserRegisteredEvent event = (UserEvent.UserRegisteredEvent) e;
                            return event.userId().equals("user-2")
                                    && event.email().equals("user@quizup.dev")
                                    && event.provider() == SocialProvider.GOOGLE
                                    && event.createdAt() != null;
                        }));
    }

    @Test
    void registerWithInvalidEmail_throwsInvalidEmailFormat() {
        UserRepositoryPort readPort = mock(UserRepositoryPort.class);
        when(readPort.existsByEmail(anyString())).thenReturn(false);

        fixture.registerInjectableResource(readPort)
                .givenNoPriorActivity()
                .when(new UserCommand.RegisterUserCommand("user-3", "invalid-email"))
                .expectException(UserProblems.InvalidEmailFormatProblem.class);
    }

    @Test
    void linkSocialProvider_appliesProviderLinkedEvent() {
        fixture.given(new UserEvent.UserRegisteredEvent(
                        "user-4", "user@quizup.dev", null, Instant.parse("2026-01-01T00:00:00Z")))
                .when(new UserCommand.LinkSocialProviderCommand("user-4", SocialProvider.GOOGLE))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        UserEvent.SocialProviderLinkedEvent.class,
                        e -> {
                            UserEvent.SocialProviderLinkedEvent event = (UserEvent.SocialProviderLinkedEvent) e;
                            return event.userId().equals("user-4")
                                    && event.provider() == SocialProvider.GOOGLE
                                    && event.linkedAt() != null;
                        }));
    }

    @Test
    void linkSocialProvider_whenAlreadyLinked_emitsNoEvent() {
        fixture.given(new UserEvent.UserRegisteredEvent(
                        "user-5", "user@quizup.dev", SocialProvider.GOOGLE, Instant.parse("2026-01-01T00:00:00Z")))
                .when(new UserCommand.LinkSocialProviderCommand("user-5", SocialProvider.GOOGLE))
                .expectNoEvents();
    }
}
