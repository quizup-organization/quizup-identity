package io.github.quizup.identity.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.identity.domain.command.UserReportCommand;
import io.github.quizup.identity.domain.event.UserReportEvent;
import io.github.quizup.identity.domain.model.UserReportContext;
import io.github.quizup.identity.domain.model.UserReportReason;
import io.github.quizup.identity.domain.model.UserReportResolution;
import io.github.quizup.identity.domain.model.UserReportResult;
import io.github.quizup.identity.domain.model.UserReportRules;
import io.github.quizup.identity.domain.model.UserReportSnapshot;
import io.github.quizup.identity.domain.model.UserReportStatus;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserReportThreadAggregateTest {

    private static final String TARGET = "target-1";
    private static final String REPORTER = "reporter-1";
    private static final String THREAD = UserReportRules.threadId(TARGET, REPORTER);
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final AggregateTestFixture<UserReportThreadAggregate> fixture =
            new AggregateTestFixture<>(UserReportThreadAggregate.class);

    @Test
    void report_createsAnOpenReport() {
        fixture.givenNoPriorActivity()
                .when(reportCommand())
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        UserReportEvent.UserReportedEvent.class,
                        e -> {
                            UserReportEvent.UserReportedEvent reported =
                                    (UserReportEvent.UserReportedEvent) e;
                            return THREAD.equals(reported.threadId())
                                    && TARGET.equals(reported.targetUserId())
                                    && REPORTER.equals(reported.reporterId())
                                    && reported.snapshot().pseudonym().equals("Target");
                        }))
                .expectResultMessagePayloadMatching(
                        org.hamcrest.Matchers.instanceOf(UserReportResult.class));
    }

    @Test
    void report_isIdempotentWhileAnOpenReportExists() {
        fixture.given(reported("report-1"))
                .when(reportCommand())
                .expectResultMessagePayload(new UserReportResult("report-1", false))
                .expectNoEvents();
    }

    @Test
    void resolve_closesTheReport() {
        fixture.given(reported("report-1"))
                .when(new UserReportCommand.ResolveUserReportCommand(
                        THREAD, "report-1", UserReportResolution.RESOLVED,
                        "admin-1", "ok", NOW.plusSeconds(10)))
                .expectEvents(new UserReportEvent.UserReportReviewedEvent(
                        THREAD, "report-1", UserReportStatus.RESOLVED,
                        "admin-1", "ok", NOW.plusSeconds(10)));
    }

    @Test
    void report_afterResolution_opensANewReport() {
        fixture.given(
                        reported("report-1"),
                        new UserReportEvent.UserReportReviewedEvent(
                                THREAD, "report-1", UserReportStatus.RESOLVED,
                                "admin-1", null, NOW.plusSeconds(5)))
                .when(reportCommand())
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        UserReportEvent.UserReportedEvent.class,
                        e -> !"report-1".equals(
                                ((UserReportEvent.UserReportedEvent) e).reportId())))
                .expectResultMessagePayloadMatching(
                        org.hamcrest.Matchers.instanceOf(UserReportResult.class));
    }

    @Test
    void threadId_isStableAndPlayerScoped() {
        assertEquals(
                UserReportRules.threadId("t", "u1"),
                UserReportRules.threadId("t", "u1"));
        assertNotEquals(
                UserReportRules.threadId("t", "u1"),
                UserReportRules.threadId("t", "u2"));
    }

    @Test
    void report_self_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> UserReportRules.validateNotSelf("u1", "u1"));
    }

    private static UserReportEvent.UserReportedEvent reported(String reportId) {
        return new UserReportEvent.UserReportedEvent(
                THREAD, reportId, TARGET, REPORTER,
                Set.of(UserReportReason.HARASSMENT), "comment",
                UserReportContext.PROFILE, null,
                new UserReportSnapshot("Target", null), NOW);
    }

    private static UserReportCommand.ReportUserCommand reportCommand() {
        return new UserReportCommand.ReportUserCommand(
                THREAD, TARGET, REPORTER,
                Set.of(UserReportReason.HARASSMENT), "comment",
                UserReportContext.PROFILE, null,
                new UserReportSnapshot("Target", null), NOW);
    }
}
