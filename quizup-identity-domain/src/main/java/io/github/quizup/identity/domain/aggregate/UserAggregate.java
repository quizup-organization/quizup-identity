package io.github.quizup.identity.domain.aggregate;

import io.github.quizup.identity.domain.command.UserCommand;
import io.github.quizup.identity.domain.event.UserEvent;
import io.github.quizup.identity.domain.exception.UserProblems;
import io.github.quizup.identity.domain.model.SocialProvider;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import lombok.Getter;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * UserAggregate - Gère le cycle de vie d'un utilisateur.
 *
 * <p>Aucun credential n'est porté par l'agrégat : l'authentification est passwordless
 * (code OTP email) ou sociale (provider OIDC).</p>
 */
@Getter
@Aggregate
public class UserAggregate {

    @AggregateIdentifier
    private String userId;

    private String email;

    private Set<SocialProvider> linkedSocialAccounts;

    // Constructeur par défaut requis par Axon
    protected UserAggregate() {
    }

    @CommandHandler
    public UserAggregate(UserCommand.RegisterUserCommand command, UserRepositoryPort userReadPort) {
        validateEmail(command.userId(), command.email(), userReadPort);

        AggregateLifecycle.apply(
                new UserEvent.UserRegisteredEvent(
                        command.userId(),
                        command.email(),
                        null,
                        Instant.now()
                )
        );
    }

    @CommandHandler
    public UserAggregate(UserCommand.RegisterUserWithSocialCommand command, UserRepositoryPort userReadPort) {
        validateProvider(command.userId(), command.provider());
        validateEmail(command.userId(), command.email(), userReadPort);

        AggregateLifecycle.apply(
                new UserEvent.UserRegisteredEvent(
                        command.userId(),
                        command.email(),
                        command.provider(),
                        Instant.now()
                )
        );
    }

    @CommandHandler
    public void handle(UserCommand.LinkSocialProviderCommand command) {
        validateProvider(command.userId(), command.provider());

        if (linkedSocialAccounts != null && linkedSocialAccounts.contains(command.provider())) {
            return;
        }

        AggregateLifecycle.apply(
                new UserEvent.SocialProviderLinkedEvent(
                        command.userId(),
                        command.provider(),
                        Instant.now()
                )
        );
    }

    @EventSourcingHandler
    public void on(UserEvent.UserRegisteredEvent event) {
        this.linkedSocialAccounts = new HashSet<>();
        this.userId = event.userId();
        this.email = event.email();

        if (event.provider() != null) {
            this.linkedSocialAccounts.add(event.provider());
        }
    }

    @EventSourcingHandler
    public void on(UserEvent.SocialProviderLinkedEvent event) {
        if (this.linkedSocialAccounts == null) {
            this.linkedSocialAccounts = new HashSet<>();
        }
        this.linkedSocialAccounts.add(event.provider());
    }

    private void validateProvider(String userId, SocialProvider provider) {
        if (provider == null) {
            throw new UserProblems.SocialProviderMissingProblem(userId);
        }
    }

    private void validateEmail(String userId, String email, UserRepositoryPort userRepositoryPort) {
        if (email == null || email.trim().isEmpty()) {
            throw new UserProblems.InvalidEmailFormatProblem(userId, email);
        }
        if (!email.contains("@")) {
            throw new UserProblems.InvalidEmailFormatProblem(userId, email);
        }
        if (userRepositoryPort.existsByEmail(email)) {
            throw new UserProblems.UserAlreadyExistsProblem(userId, email);
        }
    }
}
