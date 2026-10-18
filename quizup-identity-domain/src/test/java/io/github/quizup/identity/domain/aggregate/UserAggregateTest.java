package io.github.quizup.identity.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.identity.domain.command.UserCommand;
import io.github.quizup.identity.domain.event.UserEvent;
import io.github.quizup.identity.domain.exception.UserProblems;
import io.github.quizup.identity.domain.model.SocialProvider;
import io.github.quizup.identity.domain.model.SuspensionReinstatementReason;
import io.github.quizup.identity.domain.model.UserSuspensionReason;
import io.github.quizup.identity.domain.port.out.EmailClaimPort;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import org.axonframework.modelling.command.AggregateNotFoundException;
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
 * L'unicité d'email s'appuie désormais sur {@link EmailClaimPort} (réservation atomique).
 */
class UserAggregateTest {

    private final AggregateTestFixture<UserAggregate> fixture =
            new AggregateTestFixture<>(UserAggregate.class);

    private EmailClaimPort claimPort(boolean granted) {
        EmailClaimPort port = mock(EmailClaimPort.class);
        when(port.claim(anyString(), anyString())).thenReturn(granted);
        return port;
    }

    @Test
    void registerUser_appliesUserRegisteredEventWithoutProvider() {
        fixture.registerInjectableResource(claimPort(true))
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
        fixture.registerInjectableResource(claimPort(true))
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
        fixture.registerInjectableResource(claimPort(true))
                .givenNoPriorActivity()
                .when(new UserCommand.RegisterUserCommand("user-3", "invalid-email"))
                .expectException(UserProblems.InvalidEmailFormatProblem.class);
    }

    @Test
    void registerWithAlreadyClaimedEmail_throwsUserAlreadyExists() {
        fixture.registerInjectableResource(claimPort(false))
                .givenNoPriorActivity()
                .when(new UserCommand.RegisterUserCommand("user-6", "taken@quizup.dev"))
                .expectException(UserProblems.UserAlreadyExistsProblem.class);
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

    @Test
    void recordLogin_appliesUserLoggedInEvent() {
        fixture.given(new UserEvent.UserRegisteredEvent(
                        "user-7", "user@quizup.dev", null, Instant.parse("2026-01-01T00:00:00Z")))
                .when(new UserCommand.RecordUserLoginCommand("user-7"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        UserEvent.UserLoggedInEvent.class,
                        e -> {
                            UserEvent.UserLoggedInEvent event = (UserEvent.UserLoggedInEvent) e;
                            return event.userId().equals("user-7") && event.loggedInAt() != null;
                        }));
    }

    @Test
    void recordLogin_onUnknownUser_throwsAggregateNotFound() {
        fixture.givenNoPriorActivity()
                .when(new UserCommand.RecordUserLoginCommand("unknown-user"))
                .expectException(AggregateNotFoundException.class);
    }

    @Test
    void suspend_appliesSuspensionEvent() {
        Instant until = Instant.parse("2026-02-01T00:00:00Z");
        fixture.given(registered())
                .when(new UserCommand.SuspendUserCommand(
                        "user-8", UserSuspensionReason.HARASSMENT, "internal note",
                        "admin-1", until, Instant.parse("2026-01-01T00:00:00Z")))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        UserEvent.UserSuspendedEvent.class,
                        e -> {
                            UserEvent.UserSuspendedEvent event = (UserEvent.UserSuspendedEvent) e;
                            return event.userId().equals("user-8")
                                    && event.email().equals("user@quizup.dev")
                                    && event.suspensionId() != null
                                    && event.reason() == UserSuspensionReason.HARASSMENT
                                    && event.note().equals("internal note")
                                    && event.suspendedBy().equals("admin-1")
                                    && event.suspendedUntil().equals(until);
                        }));
    }

    @Test
    void suspend_withoutEnd_isPermanent() {
        fixture.given(registered())
                .when(new UserCommand.SuspendUserCommand(
                        "user-8", UserSuspensionReason.SPAM, null,
                        "admin-1", null, Instant.parse("2026-01-01T00:00:00Z")))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        UserEvent.UserSuspendedEvent.class,
                        e -> ((UserEvent.UserSuspendedEvent) e).suspendedUntil() == null));
    }

    @Test
    void suspend_systemAccount_isRejected() {
        fixture.given(new UserEvent.UserRegisteredEvent(
                        QuizUpConstants.SYSTEM_USER_ID, QuizUpConstants.SYSTEM_USER_EMAIL, null,
                        Instant.parse("2026-01-01T00:00:00Z")))
                .when(new UserCommand.SuspendUserCommand(
                        QuizUpConstants.SYSTEM_USER_ID, UserSuspensionReason.OTHER, null,
                        "admin-1", null, Instant.parse("2026-01-01T00:00:00Z")))
                .expectException(io.github.quizup.identity.domain.exception.UserReportProblems
                        .SystemAccountNotSuspendableProblem.class);
    }

    @Test
    void suspend_alreadySuspended_isRejected() {
        fixture.given(registered(), suspended(Instant.parse("2026-02-01T00:00:00Z")))
                .when(new UserCommand.SuspendUserCommand(
                        "user-8", UserSuspensionReason.SPAM, null,
                        "admin-1", null, Instant.parse("2026-01-15T00:00:00Z")))
                .expectException(io.github.quizup.identity.domain.exception.UserReportProblems
                        .AlreadySuspendedProblem.class);
    }

    @Test
    void suspend_afterExpiry_isAllowed() {
        fixture.given(registered(), suspended(Instant.parse("2026-01-10T00:00:00Z")))
                .when(new UserCommand.SuspendUserCommand(
                        "user-8", UserSuspensionReason.SPAM, null,
                        "admin-1", Instant.parse("2026-02-01T00:00:00Z"),
                        Instant.parse("2026-01-15T00:00:00Z")))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        UserEvent.UserSuspendedEvent.class,
                        e -> ((UserEvent.UserSuspendedEvent) e).suspendedUntil()
                                .equals(Instant.parse("2026-02-01T00:00:00Z"))));
    }

    @Test
    void reinstate_suspendedAccount_appliesEvent() {
        fixture.given(registered(), suspended(null))
                .when(new UserCommand.ReinstateUserCommand(
                        "user-8", SuspensionReinstatementReason.MANUAL, "appeal accepted",
                        "admin-1", Instant.parse("2026-01-20T00:00:00Z")))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        UserEvent.UserReinstatedEvent.class,
                        e -> {
                            UserEvent.UserReinstatedEvent event = (UserEvent.UserReinstatedEvent) e;
                            return event.userId().equals("user-8")
                                    && event.reason() == SuspensionReinstatementReason.MANUAL
                                    && event.note().equals("appeal accepted")
                                    && event.reinstatedBy().equals("admin-1")
                                    && event.reinstatedAt().equals(Instant.parse("2026-01-20T00:00:00Z"));
                        }));
    }

    @Test
    void reinstate_activeAccount_emitsNoEvent() {
        fixture.given(registered())
                .when(new UserCommand.ReinstateUserCommand(
                        "user-8", SuspensionReinstatementReason.MANUAL, null,
                        "admin-1", Instant.parse("2026-01-20T00:00:00Z")))
                .expectNoEvents();
    }

    private static UserEvent.UserRegisteredEvent registered() {
        return new UserEvent.UserRegisteredEvent(
                "user-8", "user@quizup.dev", null, Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static UserEvent.UserSuspendedEvent suspended(Instant until) {
        return new UserEvent.UserSuspendedEvent(
                "user-8", "user@quizup.dev", "suspension-1",
                UserSuspensionReason.HARASSMENT, null, "admin-0",
                Instant.parse("2026-01-05T00:00:00Z"), until);
    }
}
