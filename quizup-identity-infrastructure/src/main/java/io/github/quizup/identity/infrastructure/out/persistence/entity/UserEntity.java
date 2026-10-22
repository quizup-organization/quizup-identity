package io.github.quizup.identity.infrastructure.out.persistence.entity;

import io.github.quizup.microservice.core.domain.model.search.FieldType;
import io.github.quizup.microservice.core.domain.model.search.Searchable;
import io.github.quizup.identity.domain.model.AccountStatus;
import io.github.quizup.identity.domain.model.SocialProvider;
import io.github.quizup.identity.domain.model.SuspensionReinstatementReason;
import io.github.quizup.identity.domain.model.UserSuspensionReason;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * UserEntity - Entité JPA pour la projection du compte utilisateur
 * Cette table est une projection read-only mise à jour via les Event Handlers
 */
@Setter
@Getter
@Entity
@Table(name = "user_entry", indexes = {
    @Index(name = "idx_user_entry_email", columnList = "email"),
    @Index(name = "idx_user_entry_status_until", columnList = "status,suspended_until")
})
public class UserEntity {

    @Id
    @Column(name = "user_id", length = 255, nullable = false)
    @Searchable(type = FieldType.STRING)
    private String userId;

    @Column(name = "email", length = 255, nullable = false)
    @Searchable(type = FieldType.STRING)
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_social_providers_entry", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "provider")
    @Enumerated(EnumType.STRING)
    private Set<SocialProvider> linkedSocialAccounts = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    @Searchable(type = FieldType.DATE)
    private Instant createdAt;

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "suspension_id", length = 255)
    private String suspensionId;

    @Column(name = "suspension_reason", length = 40)
    @Enumerated(EnumType.STRING)
    private UserSuspensionReason suspensionReason;

    @Column(name = "suspension_note", length = 500)
    private String suspensionNote;

    @Column(name = "suspended_by", length = 255)
    private String suspendedBy;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspended_until")
    private Instant suspendedUntil;

    @Column(name = "reinstated_at")
    private Instant reinstatedAt;

    @Column(name = "reinstated_by", length = 255)
    private String reinstatedBy;

    @Column(name = "reinstatement_reason", length = 20)
    @Enumerated(EnumType.STRING)
    private SuspensionReinstatementReason reinstatementReason;

    @Column(name = "reinstatement_note", length = 500)
    private String reinstatementNote;
}
