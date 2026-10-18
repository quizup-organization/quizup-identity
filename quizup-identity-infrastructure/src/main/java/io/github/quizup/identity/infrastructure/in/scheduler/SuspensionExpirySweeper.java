package io.github.quizup.identity.infrastructure.in.scheduler;

import io.github.quizup.identity.domain.command.UserCommand;
import io.github.quizup.identity.domain.model.SuspensionReinstatementReason;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Réintègre automatiquement les comptes dont la suspension temporaire est échue.
 *
 * <p>Le statut effectif est déjà calculé comme {@code ACTIVE} par les lectures une fois
 * l'échéance passée : ce balayeur ne fait que produire l'événement de réintégration durable
 * (event store, audit, notification éventuelle). Il est idempotent et multi-réplique :
 * chaque instance peut envoyer la commande, l'agrégat ignore les doublons.</p>
 */
@Component
public class SuspensionExpirySweeper {

    private static final Logger logger = LoggerFactory.getLogger(SuspensionExpirySweeper.class);
    private static final int BATCH_SIZE = 100;

    private final UserRepositoryPort userRepository;
    private final CommandGateway commandGateway;

    public SuspensionExpirySweeper(UserRepositoryPort userRepository, CommandGateway commandGateway) {
        this.userRepository = userRepository;
        this.commandGateway = commandGateway;
    }

    @Scheduled(fixedDelayString = "${quizup.account-suspension.sweep-interval-ms:60000}")
    public void reinstateExpiredSuspensions() {
        Instant now = Instant.now();
        List<String> userIds = userRepository.findExpiredSuspensionIds(now, BATCH_SIZE);
        for (String userId : userIds) {
            commandGateway.send(new UserCommand.ReinstateUserCommand(
                            userId,
                            SuspensionReinstatementReason.EXPIRED,
                            "Suspension expirée",
                            QuizUpConstants.SYSTEM_USER_ID,
                            now))
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            logger.warn("Unable to reinstate expired suspension for user {}: {}",
                                    userId, error.getMessage());
                        } else {
                            logger.info("Expired suspension reinstated for user {}", userId);
                        }
                    });
        }
    }
}
