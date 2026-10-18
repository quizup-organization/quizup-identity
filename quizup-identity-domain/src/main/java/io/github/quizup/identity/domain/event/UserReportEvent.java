package io.github.quizup.identity.domain.event;

import io.github.quizup.identity.domain.model.UserReportContext;
import io.github.quizup.identity.domain.model.UserReportReason;
import io.github.quizup.identity.domain.model.UserReportSnapshot;
import io.github.quizup.identity.domain.model.UserReportStatus;

import java.time.Instant;
import java.util.Set;

public interface UserReportEvent {

    String threadId();

    record UserReportedEvent(
            String threadId,
            String reportId,
            String targetUserId,
            String reporterId,
            Set<UserReportReason> reasons,
            String comment,
            UserReportContext context,
            String gameId,
            UserReportSnapshot snapshot,
            Instant reportedAt
    ) implements UserReportEvent {
    }

    record UserReportReviewedEvent(
            String threadId,
            String reportId,
            UserReportStatus status,
            String reviewerId,
            String note,
            Instant reviewedAt
    ) implements UserReportEvent {
    }
}
