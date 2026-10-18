package io.github.quizup.identity.domain.aggregate;

import io.github.quizup.identity.domain.command.UserReportCommand;
import io.github.quizup.identity.domain.event.UserReportEvent;
import io.github.quizup.identity.domain.exception.UserReportProblems;
import io.github.quizup.identity.domain.model.UserReportResult;
import io.github.quizup.identity.domain.model.UserReportRules;
import io.github.quizup.identity.domain.model.UserReportStatus;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Thread de signalements d'un joueur sur un autre joueur.
 *
 * <p>L'identifiant est déterministe ({@link UserReportRules#threadId}) : toutes les commandes
 * d'un même couple (joueur signalé × signalant) atterrissent sur le même agrégat. Un seul
 * signalement peut être {@code OPEN} à la fois ; après clôture, un nouveau signalement peut être
 * déposé et l'historique est conservé dans le read model.</p>
 */
@Aggregate
public class UserReportThreadAggregate {

    @AggregateIdentifier
    private String threadId;
    private String targetUserId;
    private String reporterId;
    private final Map<String, UserReportStatus> reports = new HashMap<>();
    private String currentOpenReportId;

    protected UserReportThreadAggregate() {
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public UserReportResult handle(UserReportCommand.ReportUserCommand command) {
        if (currentOpenReportId != null) {
            return new UserReportResult(currentOpenReportId, false);
        }
        try {
            UserReportRules.validateNotSelf(command.targetUserId(), command.reporterId());
            UserReportRules.validateReport(command.reasons(), command.comment());
        } catch (IllegalArgumentException error) {
            throw new UserReportProblems.InvalidUserReportProblem(command.threadId(), error.getMessage());
        }

        String reportId = UUID.randomUUID().toString();
        AggregateLifecycle.apply(new UserReportEvent.UserReportedEvent(
                command.threadId(),
                reportId,
                command.targetUserId(),
                command.reporterId(),
                command.reasons(),
                command.comment(),
                command.context(),
                command.gameId(),
                command.snapshot(),
                command.reportedAt()));
        return new UserReportResult(reportId, true);
    }

    @CommandHandler
    public void handle(UserReportCommand.ResolveUserReportCommand command) {
        UserReportStatus status = reports.get(command.reportId());
        if (status == null) {
            throw new UserReportProblems.UserReportNotFoundProblem(command.reportId());
        }
        if (status != UserReportStatus.OPEN) {
            return;
        }
        try {
            UserReportRules.validateNote(command.note());
        } catch (IllegalArgumentException error) {
            throw new UserReportProblems.InvalidUserReportProblem(command.reportId(), error.getMessage());
        }

        AggregateLifecycle.apply(new UserReportEvent.UserReportReviewedEvent(
                command.threadId(),
                command.reportId(),
                command.resolution().status(),
                command.reviewerId(),
                command.note(),
                command.reviewedAt()));
    }

    @EventSourcingHandler
    public void on(UserReportEvent.UserReportedEvent event) {
        this.threadId = event.threadId();
        this.targetUserId = event.targetUserId();
        this.reporterId = event.reporterId();
        this.reports.put(event.reportId(), UserReportStatus.OPEN);
        this.currentOpenReportId = event.reportId();
    }

    @EventSourcingHandler
    public void on(UserReportEvent.UserReportReviewedEvent event) {
        this.reports.put(event.reportId(), event.status());
        if (event.reportId().equals(this.currentOpenReportId)) {
            this.currentOpenReportId = null;
        }
    }
}
