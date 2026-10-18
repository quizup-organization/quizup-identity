package io.github.quizup.identity.infrastructure.out.persistence.adapter;

import io.github.quizup.identity.domain.model.UserReport;
import io.github.quizup.identity.domain.model.UserReportCount;
import io.github.quizup.identity.domain.model.UserReportSort;
import io.github.quizup.identity.domain.model.UserReportStatus;
import io.github.quizup.identity.domain.model.UserReportSummary;
import io.github.quizup.identity.domain.model.UserReportSummaryFilter;
import io.github.quizup.identity.domain.model.UserReportSummaryPage;
import io.github.quizup.identity.domain.port.out.UserReportRepositoryPort;
import io.github.quizup.identity.infrastructure.out.persistence.entity.UserReportEntity;
import io.github.quizup.identity.infrastructure.out.persistence.mapper.UserReportEntityMapper;
import io.github.quizup.identity.infrastructure.out.persistence.repository.UserReportJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class UserReportRepositoryAdapter implements UserReportRepositoryPort {

    private final UserReportJpaRepository repository;
    private final EntityManager entityManager;

    public UserReportRepositoryAdapter(UserReportJpaRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void save(UserReport report) {
        repository.findById(report.reportId()).ifPresentOrElse(
                existing -> {
                    UserReportEntityMapper.updateEntity(existing, report);
                    repository.saveAndFlush(existing);
                },
                () -> repository.saveAndFlush(UserReportEntityMapper.toEntity(report)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserReport> findById(String reportId) {
        return repository.findById(reportId).map(UserReportEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserReport> findByTargetUserId(String targetUserId) {
        return repository.findByTargetUserIdOrderByCreatedAtDesc(targetUserId).stream()
                .map(UserReportEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserReport> findOpenByTargetUserId(String targetUserId) {
        return repository.findByTargetUserIdAndStatusOrderByCreatedAtDesc(targetUserId, UserReportStatus.OPEN)
                .stream()
                .map(UserReportEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserReportCount> findCountsByTargetUserIds(List<String> targetUserIds) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            return List.of();
        }
        List<Object[]> rows = entityManager.createQuery("""
                        SELECT r.targetUserId,
                               SUM(CASE WHEN r.status = :open THEN 1 ELSE 0 END),
                               COUNT(r),
                               MAX(r.createdAt)
                        FROM UserReportEntity r
                        WHERE r.targetUserId IN :targetUserIds
                        GROUP BY r.targetUserId
                        """, Object[].class)
                .setParameter("open", UserReportStatus.OPEN)
                .setParameter("targetUserIds", targetUserIds)
                .getResultList();
        return rows.stream()
                .map(row -> new UserReportCount(
                        (String) row[0],
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).intValue(),
                        toInstant(row[3])))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserReportSummaryPage findSummaryPage(UserReportSummaryFilter filter,
                                                 UserReportSort sort,
                                                 int page,
                                                 int size) {
        UserReportSummaryFilter effective = filter == null
                ? new UserReportSummaryFilter(UserReportStatus.OPEN, null)
                : filter;
        Instant now = Instant.now();
        Map<String, Object> parameters = new HashMap<>();
        List<String> conditions = new ArrayList<>();
        if (effective.suspended() != null) {
            String suspendedPredicate = "(u.status = 'SUSPENDED' "
                    + "AND (u.suspended_until IS NULL OR u.suspended_until > :now))";
            conditions.add(effective.suspended() ? suspendedPredicate : "NOT " + suspendedPredicate);
            parameters.put("now", now);
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String having = effective.status() == null
                ? ""
                : " HAVING SUM(CASE WHEN r.status = :status THEN 1 ELSE 0 END) > 0 ";
        if (effective.status() != null) {
            parameters.put("status", effective.status().name());
        }
        String from = " FROM user_report r JOIN user_entry u ON u.user_id = r.target_user_id ";
        String group = " GROUP BY r.target_user_id ";
        String order = switch (sort == null ? UserReportSort.RECENT_REPORT : sort) {
            case OLDEST_REPORT -> " ORDER BY last_reported_at ASC, r.target_user_id ASC ";
            case MOST_REPORTED -> " ORDER BY open_count DESC, last_reported_at DESC, r.target_user_id ASC ";
            case RECENT_REPORT -> " ORDER BY last_reported_at DESC, r.target_user_id ASC ";
        };
        String select = """
                SELECT r.target_user_id,
                       SUM(CASE WHEN r.status = 'OPEN' THEN 1 ELSE 0 END) AS open_count,
                       COUNT(*) AS total_count,
                       MAX(r.created_at) AS last_reported_at
                """;

        Query pageQuery = entityManager.createNativeQuery(select + from + where + group + having + order
                        + " LIMIT :pageSize OFFSET :offset")
                .setParameter("pageSize", size)
                .setParameter("offset", page * size);
        parameters.forEach(pageQuery::setParameter);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = pageQuery.getResultList();
        List<UserReportSummary> content = rows.stream()
                .map(row -> new UserReportSummary(
                        (String) row[0],
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).intValue(),
                        toInstant(row[3])))
                .toList();

        String countSql = "SELECT COUNT(*) FROM (SELECT r.target_user_id "
                + from + where + group + having + ") grouped_reports";
        Query countQuery = entityManager.createNativeQuery(countSql);
        parameters.forEach(countQuery::setParameter);
        long total = ((Number) countQuery.getSingleResult()).longValue();
        return new UserReportSummaryPage(content, total);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(UserReportStatus status) {
        return repository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countDistinctTargetUserIdByStatus(UserReportStatus status) {
        return repository.countDistinctTargetUserIdByStatus(status);
    }

    private static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof java.time.OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        return Instant.parse(value.toString());
    }
}
