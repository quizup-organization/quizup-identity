package io.github.quizup.identity.infrastructure.out.persistence.repository;

import io.github.quizup.identity.domain.model.UserReportStatus;
import io.github.quizup.identity.infrastructure.out.persistence.entity.UserReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserReportJpaRepository extends JpaRepository<UserReportEntity, String> {

    List<UserReportEntity> findByTargetUserIdOrderByCreatedAtDesc(String targetUserId);

    List<UserReportEntity> findByTargetUserIdAndStatusOrderByCreatedAtDesc(
            String targetUserId, UserReportStatus status);

    long countByStatus(UserReportStatus status);

    @Query("SELECT COUNT(DISTINCT r.targetUserId) FROM UserReportEntity r WHERE r.status = :status")
    long countDistinctTargetUserIdByStatus(@Param("status") UserReportStatus status);
}
