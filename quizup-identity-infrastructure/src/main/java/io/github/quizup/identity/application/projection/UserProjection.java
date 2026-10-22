package io.github.quizup.identity.application.projection;

import io.github.quizup.axon.autoconfigure.erasure.AxonEventStoreErasureService;
import io.github.quizup.identity.application.service.AccountDeletionCleanupService;
import io.github.quizup.identity.domain.event.UserEvent;
import io.github.quizup.identity.domain.model.AccountStatus;
import io.github.quizup.identity.domain.model.SocialProvider;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.model.UserSuspension;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import io.github.quizup.microservice.core.domain.event.UserAccountDeletedEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * UserProjection - Event Handlers pour maintenir les projections read-only
 */
@Component
@ProcessingGroup("user-projection")
public class UserProjection {

    private static final Logger logger = LoggerFactory.getLogger(UserProjection.class);

    private final UserRepositoryPort userRepositoryPort;
    private final AccountDeletionCleanupService deletionCleanupService;
    private final AxonEventStoreErasureService eventStoreErasureService;

    public UserProjection(UserRepositoryPort userRepositoryPort,
                          AccountDeletionCleanupService deletionCleanupService,
                          AxonEventStoreErasureService eventStoreErasureService) {
        this.userRepositoryPort = userRepositoryPort;
        this.deletionCleanupService = deletionCleanupService;
        this.eventStoreErasureService = eventStoreErasureService;
    }

    /**
     * Écoute UserRegisteredEvent et crée le profil utilisateur
     */
    @EventHandler
    @Transactional
    public void on(UserEvent.UserRegisteredEvent event) {
        if (userRepositoryPort.findById(event.userId())
                .filter(user -> user.status() == AccountStatus.DELETED)
                .isPresent()) {
            logger.warn("Ignoring registration replay for deleted user {}", event.userId());
            return;
        }
        Set<SocialProvider> linkedAccounts = new HashSet<>();

        if (event.provider() != null) {
            linkedAccounts.add(event.provider());
        }

        User user = new User(
                event.userId(),
                event.email(),
                linkedAccounts,
                event.createdAt(),
                AccountStatus.ACTIVE,
                null
        );

        userRepositoryPort.save(user);

        logger.info("User projected: userId={}", event.userId());
    }

    /**
     * Écoute SocialProviderLinkedEvent et met à jour les providers liés.
     */
    @EventHandler
    @Transactional
    public void on(UserEvent.SocialProviderLinkedEvent event) {
        userRepositoryPort.findById(event.userId()).ifPresentOrElse(user -> {
            if (user.status() == AccountStatus.DELETED) {
                return;
            }
            Set<SocialProvider> linkedAccounts = new HashSet<>(user.linkedSocialAccounts());
            linkedAccounts.add(event.provider());

            userRepositoryPort.save(new User(
                    user.userId(),
                    user.email(),
                    linkedAccounts,
                    user.createdAt(),
                    user.status(),
                    user.suspension()
            ));

            logger.info("Social provider linked: userId={}, provider={}", event.userId(), event.provider());
        }, () -> logger.warn("Cannot link social provider: user not projected yet, userId={}", event.userId()));
    }

    @EventHandler
    @Transactional
    public void on(UserEvent.UserSuspendedEvent event) {
        userRepositoryPort.findById(event.userId()).ifPresentOrElse(user -> {
            if (user.status() == AccountStatus.DELETED) {
                return;
            }
            UserSuspension suspension = new UserSuspension(
                    event.suspensionId(),
                    event.reason(),
                    event.note(),
                    event.suspendedBy(),
                    event.suspendedAt(),
                    event.suspendedUntil(),
                    null,
                    null,
                    null,
                    null);
            userRepositoryPort.save(new User(
                    user.userId(),
                    user.email(),
                    user.linkedSocialAccounts(),
                    user.createdAt(),
                    AccountStatus.SUSPENDED,
                    suspension));
            logger.info("User suspended: userId={}, until={}", event.userId(), event.suspendedUntil());
        }, () -> logger.warn("Cannot suspend: user not projected yet, userId={}", event.userId()));
    }

    @EventHandler
    @Transactional
    public void on(UserAccountDeletedEvent event) {
        User existing = userRepositoryPort.findById(event.userId()).orElse(null);
        var reportThreads = deletionCleanupService.reportThreadIds(event.userId());
        deletionCleanupService.cleanup(
                event.userId(),
                existing == null ? null : existing.email(),
                event.deletionId(),
                event.deletedAt());
        userRepositoryPort.save(new User(
                event.userId(),
                "deleted-" + event.userId() + "@invalid.local",
                new HashSet<>(),
                existing == null
                        ? (event.deletedAt() == null ? java.time.Instant.now() : event.deletedAt())
                        : existing.createdAt(),
                AccountStatus.DELETED,
                null));
        java.util.List<String> erasedAggregates = new java.util.ArrayList<>();
        erasedAggregates.add(event.userId());
        if (reportThreads != null) {
            erasedAggregates.addAll(reportThreads);
        }
        eventStoreErasureService.eraseAggregates(erasedAggregates);
        deletionCleanupService.markCompleted(event.deletionId());
        logger.info("User deleted and anonymized: userId={}, deletionId={}", event.userId(), event.deletionId());
    }

    @EventHandler
    @Transactional
    public void on(UserEvent.UserReinstatedEvent event) {
        userRepositoryPort.findById(event.userId()).ifPresentOrElse(user -> {
            if (user.status() == AccountStatus.DELETED) {
                return;
            }
            UserSuspension suspension = user.suspension();
            if (suspension != null) {
                suspension = suspension.reinstated(
                        event.reinstatedAt(),
                        event.reinstatedBy(),
                        event.reason(),
                        event.note());
            }
            userRepositoryPort.save(new User(
                    user.userId(),
                    user.email(),
                    user.linkedSocialAccounts(),
                    user.createdAt(),
                    AccountStatus.ACTIVE,
                    suspension));
            logger.info("User reinstated: userId={}, reason={}", event.userId(), event.reason());
        }, () -> logger.warn("Cannot reinstate: user not projected yet, userId={}", event.userId()));
    }
}
