package io.github.quizup.identity.infrastructure.out.persistence.repository;

import io.github.quizup.identity.domain.model.AccountStatus;
import io.github.quizup.identity.infrastructure.out.persistence.entity.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour UserEntity
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

    List<UserEntity> findAllByUserIdIn(Collection<String> userIds);

    @Query("""
            select u.userId
            from UserEntity u
            where u.status = :status
              and u.suspendedUntil is not null
              and u.suspendedUntil <= :now
            order by u.suspendedUntil asc
            """)
    List<String> findExpiredSuspensionUserIds(@Param("status") AccountStatus status,
                                              @Param("now") Instant now,
                                              Pageable pageable);

    @Query("""
            select count(u)
            from UserEntity u
            where u.status = :status
              and (u.suspendedUntil is null or u.suspendedUntil > :now)
            """)
    long countEffectivelySuspended(@Param("status") AccountStatus status,
                                   @Param("now") Instant now);
}
