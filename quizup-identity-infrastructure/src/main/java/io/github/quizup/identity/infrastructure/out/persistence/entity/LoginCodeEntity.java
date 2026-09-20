package io.github.quizup.identity.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * LoginCodeEntity - Entité JPA du code de connexion à usage unique (OTP email).
 * La clé primaire est l'email : un seul code actif par email.
 */
@Setter
@Getter
@Entity
@Table(name = "user_login_code")
public class LoginCodeEntity {

    @Id
    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "code_hash", length = 255, nullable = false)
    private String codeHash;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;
}
