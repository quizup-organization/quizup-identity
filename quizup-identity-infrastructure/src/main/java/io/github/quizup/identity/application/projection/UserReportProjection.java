package io.github.quizup.identity.application.projection;

import io.github.quizup.identity.domain.event.UserReportEvent;
import io.github.quizup.identity.domain.model.UserReport;
import io.github.quizup.identity.domain.model.UserReportStatus;
import io.github.quizup.identity.domain.port.out.UserReportRepositoryPort;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Projection des signalements de joueurs.
 */
@Component
@ProcessingGroup("user-report-projection")
public class UserReportProjection {

    private final UserReportRepositoryPort reportRepository;

    public UserReportProjection(UserReportRepositoryPort reportRepository) {
        this.reportRepository = reportRepository;
    }

    @EventHandler
    @Transactional
    public void on(UserReportEvent.UserReportedEvent event) {
        reportRepository.save(UserReport.builder()
                .reportId(event.reportId())
                .threadId(event.threadId())
                .targetUserId(event.targetUserId())
                .reporterId(event.reporterId())
                .reasons(event.reasons())
                .comment(event.comment())
                .context(event.context())
                .gameId(event.gameId())
                .snapshot(event.snapshot())
                .status(UserReportStatus.OPEN)
                .createdAt(event.reportedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(UserReportEvent.UserReportReviewedEvent event) {
        reportRepository.findById(event.reportId()).ifPresent(report -> reportRepository.save(
                report.toBuilder()
                        .status(event.status())
                        .reviewedBy(event.reviewerId())
                        .reviewedAt(event.reviewedAt())
                        .resolutionNote(event.note())
                        .build()));
    }
}
