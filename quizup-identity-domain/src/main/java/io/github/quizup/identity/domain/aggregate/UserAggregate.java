package io.github.quizup.identity.domain.aggregate;

import io.github.quizup.identity.domain.command.UserCommand;
import io.github.quizup.identity.domain.event.UserEvent;
import io.github.quizup.identity.domain.exception.UserProblems;
import io.github.quizup.identity.domain.exception.UserReportProblems;
import io.github.quizup.identity.domain.model.AccountStatus;
import io.github.quizup.identity.domain.model.AccountStatusRules;
import io.github.quizup.identity.domain.model.SocialProvider;
import io.github.quizup.identity.domain.model.UserReportRules;
import io.github.quizup.identity.domain.model.UserSuspension;
import io.github.quizup.identity.domain.port.out.EmailClaimPort;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.identity.domain.event.UserEvent.UserAccountDeletedEvent;
import lombok.Getter;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * UserAggregate - Gère le cycle de vie d'un utilisateur.
 *
 * <p>Aucun credential n'est porté par l'agrégat : l'authentification est passwordless
 * (code OTP email) ou sociale (provider OIDC). L'agrégat porte aussi le statut de compte
 * ({@link AccountStatus}) et la dernière suspension connue.</p>
 */
@Getter
@Aggregate
public class UserAggregate {

    @AggregateIdentifier
    private String userId;

    private String email;

    private Set<SocialProvider> linkedSocialAccounts;

    private Instant lastLoginAt;

    private AccountStatus status = AccountStatus.ACTIVE;

    private UserSuspension suspension;

    // Constructeur par défaut requis par Axon
    protected UserAggregate() {
    }

    @CommandHandler
    public UserAggregate(UserCommand.RegisterUserCommand command, EmailClaimPort emailClaimPort) {
        validateRegistration(command.userId(), command.email(), emailClaimPort);

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
    public UserAggregate(UserCommand.RegisterUserWithSocialCommand command, EmailClaimPort emailClaimPort) {
        validateProvider(command.userId(), command.provider());
        validateRegistration(command.userId(), command.email(), emailClaimPort);

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

    @CommandHandler
    public void handle(UserCommand.RecordUserLoginCommand command) {
        AggregateLifecycle.apply(
                new UserEvent.UserLoggedInEvent(
                        command.userId(),
                        Instant.now()
                )
        );
    }

    @CommandHandler
    public void handle(UserCommand.DeleteAccountCommand command,
                       EmailClaimPort emailClaimPort) {
        if (QuizUpConstants.SYSTEM_USER_ID.equals(userId)) {
            throw new UserReportProblems.SystemAccountNotSuspendableProblem(userId);
        }
        if (status == AccountStatus.DELETED) {
            return;
        }
        if (email != null) {
            emailClaimPort.release(email, userId);
        }
        AggregateLifecycle.apply(new UserAccountDeletedEvent(
                userId,
                command.deletionId(),
                command.deletedAt()));
    }

    @CommandHandler
    public void handle(UserCommand.SuspendUserCommand command) {
        if (QuizUpConstants.SYSTEM_USER_ID.equals(userId)) {
            throw new UserReportProblems.SystemAccountNotSuspendableProblem(userId);
        }
        if (status == AccountStatus.SUSPENDED
                && AccountStatusRules.isSuspended(status,
                suspension == null ? null : suspension.suspendedUntil(), command.suspendedAt())) {
            throw new UserReportProblems.AlreadySuspendedProblem(userId);
        }
        if (command.reason() == null) {
            throw new UserReportProblems.InvalidSuspensionProblem(userId, "A suspension reason is required");
        }
        try {
            UserReportRules.validateNote(command.note());
            UserReportRules.validateSuspension(command.suspendedAt(), command.suspendedUntil());
        } catch (IllegalArgumentException error) {
            throw new UserReportProblems.InvalidSuspensionProblem(userId, error.getMessage());
        }

        AggregateLifecycle.apply(new UserEvent.UserSuspendedEvent(
                userId,
                email,
                UUID.randomUUID().toString(),
                command.reason(),
                command.note(),
                command.requestedBy(),
                command.suspendedAt(),
                command.suspendedUntil()));
    }

    @CommandHandler
    public void handle(UserCommand.ReinstateUserCommand command) {
        if (status != AccountStatus.SUSPENDED) {
            return;
        }
        try {
            UserReportRules.validateNote(command.note());
        } catch (IllegalArgumentException error) {
            throw new UserReportProblems.InvalidSuspensionProblem(userId, error.getMessage());
        }

        AggregateLifecycle.apply(new UserEvent.UserReinstatedEvent(
                userId,
                email,
                suspension == null ? null : suspension.suspensionId(),
                command.reason(),
                command.note(),
                command.requestedBy(),
                command.reinstatedAt()));
    }

    @EventSourcingHandler
    public void on(UserEvent.UserRegisteredEvent event) {
        this.linkedSocialAccounts = new HashSet<>();
        this.userId = event.userId();
        this.email = event.email();
        this.status = AccountStatus.ACTIVE;

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

    @EventSourcingHandler
    public void on(UserEvent.UserLoggedInEvent event) {
        this.lastLoginAt = event.loggedInAt();
    }

    @EventSourcingHandler
    public void on(UserEvent.UserSuspendedEvent event) {
        this.status = AccountStatus.SUSPENDED;
        this.suspension = new UserSuspension(
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
    }

    @EventSourcingHandler
    public void on(UserAccountDeletedEvent event) {
        this.status = AccountStatus.DELETED;
        this.email = "deleted-" + userId + "@invalid.local";
        this.linkedSocialAccounts = new HashSet<>();
        this.suspension = null;
        this.lastLoginAt = null;
    }

    @EventSourcingHandler
    public void on(UserEvent.UserReinstatedEvent event) {
        this.status = AccountStatus.ACTIVE;
        if (this.suspension != null) {
            this.suspension = this.suspension.reinstated(
                    event.reinstatedAt(),
                    event.reinstatedBy(),
                    event.reason(),
                    event.note());
        }
    }

    private void validateProvider(String userId, SocialProvider provider) {
        if (provider == null) {
            throw new UserProblems.SocialProviderMissingProblem(userId);
        }
    }

    private void validateRegistration(String userId, String email, EmailClaimPort emailClaimPort) {
        if (email == null || email.trim().isEmpty()) {
            throw new UserProblems.InvalidEmailFormatProblem(userId, email);
        }
        if (!email.contains("@")) {
            throw new UserProblems.InvalidEmailFormatProblem(userId, email);
        }
        if (!emailClaimPort.claim(email, userId)) {
            throw new UserProblems.UserAlreadyExistsProblem(userId, email);
        }
    }
}
