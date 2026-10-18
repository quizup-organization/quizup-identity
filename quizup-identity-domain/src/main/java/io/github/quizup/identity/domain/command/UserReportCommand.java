package io.github.quizup.identity.domain.command;

import io.github.quizup.identity.domain.model.UserReportContext;
import io.github.quizup.identity.domain.model.UserReportReason;
import io.github.quizup.identity.domain.model.UserReportResolution;
import io.github.quizup.identity.domain.model.UserReportSnapshot;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.time.Instant;
import java.util.Set;

public interface UserReportCommand {

    record ReportUserCommand(
            @TargetAggregateIdentifier String threadId,
            String targetUserId,
            String reporterId,
            Set<UserReportReason> reasons,
            String comment,
            UserReportContext context,
            String gameId,
            UserReportSnapshot snapshot,
            Instant reportedAt
    ) implements UserReportCommand {
    }

    record ResolveUserReportCommand(
            @TargetAggregateIdentifier String threadId,
            String reportId,
            UserReportResolution resolution,
            String reviewerId,
            String note,
            Instant reviewedAt
    ) implements UserReportCommand {
    }
}
