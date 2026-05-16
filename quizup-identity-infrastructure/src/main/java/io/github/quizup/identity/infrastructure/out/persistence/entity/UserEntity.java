package io.github.quizup.identity.infrastructure.out.persistence.entity;

import io.github.quizup.common.domain.model.search.FieldType;
import io.github.quizup.common.domain.model.search.Searchable;
import io.github.quizup.identity.domain.model.SocialProvider;
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
    @Index(name = "idx_user_entry_email", columnList = "email")
})
public class UserEntity {

    @Id
    @Column(name = "user_id", length = 255, nullable = false)
    @Searchable(type = FieldType.STRING)
    private String userId;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    @Searchable(type = FieldType.STRING)
    private String email;

    @Column(name = "password", length = 255)
    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_social_providers_entry", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "provider")
    @Enumerated(EnumType.STRING)
    private Set<SocialProvider> linkedSocialAccounts = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    @Searchable(type = FieldType.DATE)
    private Instant createdAt;
}
