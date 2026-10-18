package io.github.quizup.identity.application.service;

import io.github.quizup.identity.domain.model.AccountStatus;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.model.UserSuspension;
import io.github.quizup.identity.domain.model.UserSuspensionReason;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AccountAccessRevocationServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final UserRepositoryPort repository = mock(UserRepositoryPort.class);
    private final AccountAccessRevocationService service =
            new AccountAccessRevocationService(jdbcTemplate, repository);

    @Test
    void activeAccount_isNotRevoked() {
        when(repository.findById("user-1")).thenReturn(Optional.of(new User(
                "user-1", "user@quizup.dev", Set.of(), Instant.now())));

        service.revokeIfCurrent("user-1", "s1");

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void staleSuspension_isNotRevoked() {
        when(repository.findById("user-1")).thenReturn(Optional.of(suspended("s2")));

        service.revokeIfCurrent("user-1", "s1");

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void currentSuspension_revokesSessionsAndAuthorizations() {
        when(repository.findById("user-1")).thenReturn(Optional.of(suspended("s1")));

        service.revokeIfCurrent("user-1", "s1");

        verify(jdbcTemplate).update(
                "DELETE FROM spring_session WHERE principal_name = ?", "user-1");
        verify(jdbcTemplate).update(
                "DELETE FROM oauth2_authorization WHERE principal_name = ?", "user-1");
    }

    private static User suspended(String suspensionId) {
        Instant now = Instant.now();
        return new User(
                "user-1", "user@quizup.dev", Set.of(), now, AccountStatus.SUSPENDED,
                new UserSuspension(suspensionId, UserSuspensionReason.SPAM, null, "admin-1",
                        now, null, null, null, null, null));
    }
}
