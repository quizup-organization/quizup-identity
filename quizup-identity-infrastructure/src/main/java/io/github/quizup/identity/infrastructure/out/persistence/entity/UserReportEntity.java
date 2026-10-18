package io.github.quizup.identity.infrastructure.out.persistence.entity;

import io.github.quizup.identity.domain.model.UserReportContext;
import io.github.quizup.identity.domain.model.UserReportReason;
import io.github.quizup.identity.domain.model.UserReportStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/** Projection read-only d'un signalement de joueur. */
@Setter
@Getter
@Entity
@Table(name = "user_report", indexes = {
        @Index(name = "idx_user_report_target_status", columnList = "target_user_id,status"),
        @Index(name = "idx_user_report_status_created", columnList = "status,created_at"),
        @Index(name = "idx_user_report_thread", columnList = "thread_id")
})
public class UserReportEntity {

    @Id
    @Column(name = "report_id", length = 255, nullable = false)
    private String reportId;

    @Column(name = "thread_id", length = 255, nullable = false)
    private String threadId;

    @Column(name = "target_user_id", length = 255, nullable = false)
    private String targetUserId;

    @Column(name = "reporter_id", length = 255, nullable = false)
    private String reporterId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_report_reason", joinColumns = @JoinColumn(name = "report_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 32, nullable = false)
    private Set<UserReportReason> reasons = new HashSet<>();

    @Column(name = "comment", length = 500)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "context", length = 20, nullable = false)
    private UserReportContext context;

    @Column(name = "game_id", length = 255)
    private String gameId;

    @Column(name = "target_pseudonym", length = 100)
    private String targetPseudonym;

    @Column(name = "target_avatar_options", columnDefinition = "TEXT")
    private String targetAvatarOptions;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private UserReportStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "reviewed_by", length = 255)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;
}
