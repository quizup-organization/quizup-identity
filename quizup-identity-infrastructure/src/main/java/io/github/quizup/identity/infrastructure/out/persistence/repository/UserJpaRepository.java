package io.github.quizup.identity.infrastructure.out.persistence.repository;

import io.github.quizup.identity.infrastructure.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour UserProfileEntity
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, String>, JpaSpecificationExecutor<UserEntity> {

    /**
     * Trouve un profil par email
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Vérifie si un email existe déjà
     */
    boolean existsByEmail(String email);
}
