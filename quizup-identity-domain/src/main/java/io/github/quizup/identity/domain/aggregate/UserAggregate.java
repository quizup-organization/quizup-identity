package io.github.quizup.identity.domain.aggregate;

import io.github.quizup.identity.domain.command.UserCommand;
import io.github.quizup.identity.domain.event.UserEvent;
import io.github.quizup.identity.domain.exception.UserProblems;
import io.github.quizup.identity.domain.model.NameGenerator;
import io.github.quizup.identity.domain.model.SocialProvider;
import io.github.quizup.identity.domain.port.out.PasswordEncoderPort;
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
 * UserAggregate - Gère le cycle de vie d'un utilisateur
 */
@Getter
@Aggregate
public class UserAggregate {

    @AggregateIdentifier
    private String userId;

    private String email;
    private String password;

    private String name;

    private Set<SocialProvider> linkedSocialAccounts;

    // Constructeur par défaut requis par Axon
    protected UserAggregate() {
    }

    @CommandHandler
    public UserAggregate(UserCommand.RegisterUserWithPasswordCommand command,
                         PasswordEncoderPort passwordEncoderPort,
                         UserRepositoryPort userReadPort) {
        validateEmail(command.userId(), command.email(), userReadPort);
        validatePassword(command.userId(), command.email(), command.password());

        AggregateLifecycle.apply(
                new UserEvent.UserRegisteredEvent(
                        command.userId(),
                        command.email(),
                        passwordEncoderPort.encode(command.password()),
                        NameGenerator.generate(),
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
                        null,
                        NameGenerator.generate(),
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
        this.password = event.password();
        this.name = event.name();

        if (event.provider() != null) {
            this.linkedSocialAccounts.add(event.provider());
        }
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

    private void validatePassword(String userId, String email, String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new UserProblems.InvalidPasswordFormatProblem(userId, email, password);
        }
    }
}
