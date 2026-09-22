package io.github.quizup.identity.application.projection;

import io.github.quizup.identity.domain.event.UserEvent;
import io.github.quizup.identity.domain.model.SocialProvider;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
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

    public UserProjection(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    /**
     * Écoute UserRegisteredEvent et crée le profil utilisateur
     */
    @EventHandler
    @Transactional
    public void on(UserEvent.UserRegisteredEvent event) {
        Set<SocialProvider> linkedAccounts = new HashSet<>();

        if (event.provider() != null) {
            linkedAccounts.add(event.provider());
        }

        User user = new User(
                event.userId(),
                event.email(),
                linkedAccounts,
                event.createdAt()
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
            Set<SocialProvider> linkedAccounts = new HashSet<>(user.linkedSocialAccounts());
            linkedAccounts.add(event.provider());

            userRepositoryPort.save(new User(
                    user.userId(),
                    user.email(),
                    linkedAccounts,
                    user.createdAt()
            ));

            logger.info("Social provider linked: userId={}, provider={}", event.userId(), event.provider());
        }, () -> logger.warn("Cannot link social provider: user not projected yet, userId={}", event.userId()));
    }
}
