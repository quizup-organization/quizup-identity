package io.github.quizup.identity.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Réservation d'email (write-side) : contrainte unique qui empêche deux inscriptions
 * concurrentes sur le même email. Sur suppression d'un compte, la ligne est retirée par le domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "email_claim")
public class EmailClaimEntity {

    @Id
    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "user_id", length = 255, nullable = false)
    private String userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public EmailClaimEntity(String email, String userId, Instant createdAt) {
        this.email = email;
        this.userId = userId;
        this.createdAt = createdAt;
    }
}
