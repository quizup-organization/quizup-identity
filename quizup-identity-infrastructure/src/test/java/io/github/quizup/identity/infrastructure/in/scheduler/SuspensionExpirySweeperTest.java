package io.github.quizup.identity.infrastructure.in.scheduler;

import io.github.quizup.identity.domain.command.UserCommand;
import io.github.quizup.identity.domain.model.SuspensionReinstatementReason;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuspensionExpirySweeperTest {

    private final UserRepositoryPort repository = mock(UserRepositoryPort.class);
    private final CommandGateway commandGateway = mock(CommandGateway.class);
    private final SuspensionExpirySweeper sweeper =
            new SuspensionExpirySweeper(repository, commandGateway);

    @Test
    void expiredSuspensionsAreReinstatedWithSystemActor() {
        when(repository.findExpiredSuspensionIds(any(), anyInt())).thenReturn(List.of("user-1"));
        when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        sweeper.reinstateExpiredSuspensions();

        ArgumentCaptor<UserCommand.ReinstateUserCommand> captor =
                ArgumentCaptor.forClass(UserCommand.ReinstateUserCommand.class);
        verify(commandGateway).send(captor.capture());
        UserCommand.ReinstateUserCommand command = captor.getValue();
        assertThat(command.userId()).isEqualTo("user-1");
        assertThat(command.reason()).isEqualTo(SuspensionReinstatementReason.EXPIRED);
        assertThat(command.requestedBy())
                .isEqualTo(io.github.quizup.microservice.core.domain.constant.QuizUpConstants.SYSTEM_USER_ID);
    }

    @Test
    void noExpiredSuspension_sendsNoCommand() {
        when(repository.findExpiredSuspensionIds(any(), anyInt())).thenReturn(List.of());

        sweeper.reinstateExpiredSuspensions();

        verify(commandGateway, org.mockito.Mockito.never()).send(any());
    }
}
