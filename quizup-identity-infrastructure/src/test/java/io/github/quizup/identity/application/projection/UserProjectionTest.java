package io.github.quizup.identity.application.projection;

import io.github.quizup.identity.domain.event.UserEvent;
import io.github.quizup.identity.domain.model.AccountStatus;
import io.github.quizup.identity.domain.model.SuspensionReinstatementReason;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.model.UserSuspension;
import io.github.quizup.identity.domain.model.UserSuspensionReason;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProjectionTest {

    private static final Instant AT = Instant.parse("2026-10-03T18:02:00Z");

    private final UserRepositoryPort repository = mock(UserRepositoryPort.class);
    private final UserProjection projection = new UserProjection(repository);

    @Test
    void suspension_updatesStatusAndMetadata() {
        when(repository.findById("user-1")).thenReturn(Optional.of(activeUser()));

        projection.on(new UserEvent.UserSuspendedEvent(
                "user-1", "user@quizup.dev", "s1",
                UserSuspensionReason.HARASSMENT, "internal", "admin-1", AT, null));

        User saved = captureSaved();
        assertThat(saved.status()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(saved.suspension().suspensionId()).isEqualTo("s1");
        assertThat(saved.suspension().reason()).isEqualTo(UserSuspensionReason.HARASSMENT);
        assertThat(saved.suspension().permanent()).isTrue();
    }

    @Test
    void reinstatement_returnsToActiveAndKeepsHistory() {
        User suspended = new User(
                "user-1", "user@quizup.dev", Set.of(), AT, AccountStatus.SUSPENDED,
                new UserSuspension("s1", UserSuspensionReason.SPAM, "note", "admin-1",
                        AT, null, null, null, null, null));
        when(repository.findById("user-1")).thenReturn(Optional.of(suspended));

        projection.on(new UserEvent.UserReinstatedEvent(
                "user-1", "user@quizup.dev", "s1",
                SuspensionReinstatementReason.MANUAL, "appeal", "admin-1", AT.plusSeconds(60)));

        User saved = captureSaved();
        assertThat(saved.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.suspension().reinstatedAt()).isEqualTo(AT.plusSeconds(60));
        assertThat(saved.suspension().reinstatedBy()).isEqualTo("admin-1");
        assertThat(saved.suspension().reinstatementNote()).isEqualTo("appeal");
    }

    @Test
    void socialProviderLink_preservesSuspensionStatus() {
        User suspended = new User(
                "user-1", "user@quizup.dev", Set.of(), AT, AccountStatus.SUSPENDED,
                new UserSuspension("s1", UserSuspensionReason.SPAM, null, "admin-1",
                        AT, null, null, null, null, null));
        when(repository.findById("user-1")).thenReturn(Optional.of(suspended));

        projection.on(new UserEvent.SocialProviderLinkedEvent(
                "user-1", io.github.quizup.identity.domain.model.SocialProvider.GOOGLE, AT));

        User saved = captureSaved();
        assertThat(saved.status()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(saved.suspension().suspensionId()).isEqualTo("s1");
    }

    private User captureSaved() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private static User activeUser() {
        return new User("user-1", "user@quizup.dev", Set.of(), AT);
    }
}
