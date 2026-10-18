package io.github.quizup.identity.application.handler.query;

import io.github.quizup.identity.domain.exception.UserReportProblems;
import io.github.quizup.identity.domain.model.UserReport;
import io.github.quizup.identity.domain.model.UserReportCount;
import io.github.quizup.identity.domain.model.UserReportStats;
import io.github.quizup.identity.domain.model.UserReportStatus;
import io.github.quizup.identity.domain.model.UserReportSummaryPage;
import io.github.quizup.identity.domain.port.out.UserReportRepositoryPort;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import io.github.quizup.identity.domain.query.UserReportQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Handler Axon des queries de signalements de joueurs.
 */
@Component
public class UserReportQueryHandler {

    private final UserReportRepositoryPort reportRepository;
    private final UserRepositoryPort userRepository;

    public UserReportQueryHandler(UserReportRepositoryPort reportRepository,
                                  UserRepositoryPort userRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    @QueryHandler
    public UserReport handle(UserReportQuery.GetUserReportByIdQuery query) {
        return reportRepository.findById(query.reportId())
                .orElseThrow(() -> new UserReportProblems.UserReportNotFoundProblem(query.reportId()));
    }

    @QueryHandler
    public List<UserReport> handle(UserReportQuery.GetUserReportsByTargetQuery query) {
        return reportRepository.findByTargetUserId(query.targetUserId());
    }

    @QueryHandler
    public List<UserReportCount> handle(UserReportQuery.GetUserReportCountsQuery query) {
        return reportRepository.findCountsByTargetUserIds(query.targetUserIds());
    }

    @QueryHandler
    public UserReportSummaryPage handle(UserReportQuery.GetReportedUserPageQuery query) {
        return reportRepository.findSummaryPage(query.filter(), query.sort(), query.page(), query.size());
    }

    @QueryHandler
    public UserReportStats handle(UserReportQuery.GetUserReportStatsQuery query) {
        return new UserReportStats(
                reportRepository.countByStatus(UserReportStatus.OPEN),
                reportRepository.countDistinctTargetUserIdByStatus(UserReportStatus.OPEN),
                userRepository.countSuspended(Instant.now()));
    }
}
