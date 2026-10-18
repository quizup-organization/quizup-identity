package io.github.quizup.identity.infrastructure.out.persistence.adapter;

import io.github.quizup.microservice.core.infrastructure.in.api.request.FilterRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SortRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SortResponse;
import io.github.quizup.microservice.core.domain.model.search.FilterOperator;
import io.github.quizup.microservice.core.domain.model.search.SearchableEntity;
import io.github.quizup.microservice.core.domain.model.search.SortDirection;
import io.github.quizup.microservice.core.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.microservice.core.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.microservice.core.infrastructure.adapter.SearchRequestSpecification;
import io.github.quizup.microservice.core.infrastructure.adapter.SearchRequestValidator;
import io.github.quizup.identity.domain.model.AccountStatus;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.model.UserReportStatus;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import io.github.quizup.identity.infrastructure.out.persistence.entity.UserEntity;
import io.github.quizup.identity.infrastructure.out.persistence.entity.UserReportEntity;
import io.github.quizup.identity.infrastructure.out.persistence.mapper.UserEntityMapper;
import io.github.quizup.identity.infrastructure.out.persistence.repository.UserJpaRepository;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    /**
     * Filtres admin hors modèle {@code @Searchable} : traités par la spécification dédiée
     * (statut de suspension effectif et présence de signalements ouverts).
     */
    private static final String ADMIN_STATUS_FILTER = "status";
    private static final String ADMIN_REPORTED_FILTER = "reported";

    private final UserJpaRepository userJpaRepository;
    private final SearchableEntity searchableEntity;
    private final JpaSearchAdapter<UserEntity> userJpaSearchAdapter;

    public UserRepositoryAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
        this.searchableEntity = new AnnotationSearchableEntity(UserEntity.class);
        this.userJpaSearchAdapter = new JpaSearchAdapter<>(userJpaRepository, searchableEntity);
    }

    @Override
    public void save(User user) {
        userJpaRepository.save(UserEntityMapper.toEntity(user));
    }

    @Override
    public Optional<User> findById(String userId) {
        return userJpaRepository.findById(userId).map(UserEntityMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(UserEntityMapper::toDomain);
    }

    @Override
    public List<User> findAllByIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userJpaRepository.findAllByUserIdIn(userIds).stream()
                .map(UserEntityMapper::toDomain)
                .toList();
    }

    @Override
    public SearchResponse<User> findAll(SearchRequest request) {
        List<FilterRequest> filters = request == null || request.filters() == null
                ? List.of()
                : request.filters();
        List<FilterRequest> adminFilters = filters.stream()
                .filter(UserRepositoryAdapter::isAdminFilter)
                .toList();
        if (adminFilters.isEmpty()) {
            return userJpaSearchAdapter.findAll(request).map(UserEntityMapper::toDomain);
        }

        List<FilterRequest> standardFilters = filters.stream()
                .filter(filter -> !isAdminFilter(filter))
                .toList();
        SearchRequest standardRequest = new SearchRequest(
                standardFilters, request.sorts(), request.page());
        SearchRequestValidator.validate(searchableEntity, standardRequest);

        Specification<UserEntity> standardSpecification =
                new SearchRequestSpecification<>(standardRequest, searchableEntity);
        Specification<UserEntity> specification =
                standardSpecification.and(adminFilterSpecification(adminFilters));
        Page<UserEntity> page = userJpaRepository.findAll(
                specification, toPageable(request.page(), request.sorts()));
        return toSearchResponse(page);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsById(String userId) {
        return userJpaRepository.existsById(userId);
    }

    @Override
    public List<String> findExpiredSuspensionIds(Instant now, int limit) {
        return userJpaRepository.findExpiredSuspensionUserIds(
                AccountStatus.SUSPENDED, now, org.springframework.data.domain.PageRequest.of(0, Math.max(1, limit)));
    }

    @Override
    public long countSuspended(Instant now) {
        return userJpaRepository.countEffectivelySuspended(AccountStatus.SUSPENDED, now);
    }

    private static boolean isAdminFilter(FilterRequest filter) {
        return filter != null
                && filter.operator() == FilterOperator.EQUALS
                && (ADMIN_STATUS_FILTER.equals(filter.property())
                || ADMIN_REPORTED_FILTER.equals(filter.property()));
    }

    /**
     * Le statut admin porte sur la suspension <b>effective</b> : un compte suspendu dont
     * l'échéance est passée est considéré actif, même avant le passage du balayeur.
     */
    private static Specification<UserEntity> adminFilterSpecification(List<FilterRequest> filters) {
        Instant now = Instant.now();
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (FilterRequest filter : filters) {
                if (ADMIN_STATUS_FILTER.equals(filter.property())) {
                    AccountStatus status = AccountStatus.valueOf(String.valueOf(filter.value()));
                    Path<AccountStatus> statusPath = root.get("status");
                    Path<Instant> suspendedUntilPath = root.get("suspendedUntil");
                    Predicate suspended = cb.and(
                            cb.equal(statusPath, AccountStatus.SUSPENDED),
                            cb.or(
                                    cb.isNull(suspendedUntilPath),
                                    cb.greaterThan(suspendedUntilPath, now)));
                    predicates.add(status == AccountStatus.SUSPENDED
                            ? suspended
                            : cb.not(suspended));
                } else if (ADMIN_REPORTED_FILTER.equals(filter.property())
                        && Boolean.TRUE.equals(filter.value())) {
                    Subquery<String> reportedIds = query.subquery(String.class);
                    Root<UserReportEntity> report = reportedIds.from(UserReportEntity.class);
                    reportedIds.select(report.get("targetUserId"))
                            .where(cb.equal(report.get("status"), UserReportStatus.OPEN));
                    predicates.add(root.get("userId").in(reportedIds));
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Pageable toPageable(
            io.github.quizup.microservice.core.infrastructure.in.api.request.PageRequest page,
            List<SortRequest> sorts) {
        if (page == null) {
            return Pageable.unpaged();
        }
        int number = page.number() == null ? 0 : page.number();
        int size = page.size() == null ? 10 : page.size();
        return org.springframework.data.domain.PageRequest.of(number, size, toSort(sorts));
    }

    private static Sort toSort(List<SortRequest> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return Sort.unsorted();
        }
        List<Sort.Order> orders = sorts.stream()
                .filter(sort -> sort != null && sort.property() != null && !sort.property().isBlank())
                .map(sort -> sort.direction() == SortDirection.DESC
                        ? Sort.Order.desc(sort.property())
                        : Sort.Order.asc(sort.property()))
                .toList();
        return Sort.by(orders);
    }

    private static SearchResponse<User> toSearchResponse(Page<UserEntity> page) {
        List<SortResponse> sorts = page.getSort().stream()
                .map(order -> new SortResponse(
                        order.getProperty(),
                        order.isAscending() ? SortDirection.ASC : SortDirection.DESC))
                .toList();
        return new SearchResponse<>(
                page.getContent().stream().map(UserEntityMapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                sorts,
                page.isFirst(),
                page.isLast(),
                page.isEmpty());
    }
}
