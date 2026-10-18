package io.github.quizup.identity.infrastructure.out.persistence.mapper;

import io.github.quizup.identity.domain.model.UserReport;
import io.github.quizup.identity.domain.model.UserReportSnapshot;
import io.github.quizup.identity.infrastructure.out.persistence.entity.UserReportEntity;

import java.util.HashSet;

public final class UserReportEntityMapper {

    private UserReportEntityMapper() {
    }

    public static UserReport toDomain(UserReportEntity entity) {
        return UserReport.builder()
                .reportId(entity.getReportId())
                .threadId(entity.getThreadId())
                .targetUserId(entity.getTargetUserId())
                .reporterId(entity.getReporterId())
                .reasons(new HashSet<>(entity.getReasons()))
                .comment(entity.getComment())
                .context(entity.getContext())
                .gameId(entity.getGameId())
                .snapshot(entity.getTargetPseudonym() == null && entity.getTargetAvatarOptions() == null
                        ? null
                        : new UserReportSnapshot(entity.getTargetPseudonym(), entity.getTargetAvatarOptions()))
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .reviewedBy(entity.getReviewedBy())
                .reviewedAt(entity.getReviewedAt())
                .resolutionNote(entity.getResolutionNote())
                .build();
    }

    public static UserReportEntity toEntity(UserReport report) {
        UserReportEntity entity = new UserReportEntity();
        entity.setReportId(report.reportId());
        entity.setThreadId(report.threadId());
        entity.setTargetUserId(report.targetUserId());
        entity.setReporterId(report.reporterId());
        entity.getReasons().clear();
        entity.getReasons().addAll(report.reasons());
        entity.setComment(report.comment());
        entity.setContext(report.context());
        entity.setGameId(report.gameId());
        entity.setTargetPseudonym(report.snapshot() == null ? null : report.snapshot().pseudonym());
        entity.setTargetAvatarOptions(report.snapshot() == null ? null : report.snapshot().avatarOptions());
        entity.setStatus(report.status());
        entity.setCreatedAt(report.createdAt());
        entity.setReviewedBy(report.reviewedBy());
        entity.setReviewedAt(report.reviewedAt());
        entity.setResolutionNote(report.resolutionNote());
        return entity;
    }

    public static void updateEntity(UserReportEntity entity, UserReport report) {
        entity.setComment(report.comment());
        entity.setContext(report.context());
        entity.setGameId(report.gameId());
        entity.setTargetPseudonym(report.snapshot() == null ? null : report.snapshot().pseudonym());
        entity.setTargetAvatarOptions(report.snapshot() == null ? null : report.snapshot().avatarOptions());
        entity.setStatus(report.status());
        entity.setReviewedBy(report.reviewedBy());
        entity.setReviewedAt(report.reviewedAt());
        entity.setResolutionNote(report.resolutionNote());
    }
}
