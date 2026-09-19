package io.github.quizup.identity.infrastructure.out.persistence.adapter;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.microservice.core.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.microservice.core.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import io.github.quizup.identity.infrastructure.out.persistence.entity.UserEntity;
import io.github.quizup.identity.infrastructure.out.persistence.mapper.UserEntityMapper;
import io.github.quizup.identity.infrastructure.out.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;
    private final JpaSearchAdapter<UserEntity> userJpaSearchAdapter;

    public UserRepositoryAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
        this.userJpaSearchAdapter = new JpaSearchAdapter<>(userJpaRepository, new AnnotationSearchableEntity(UserEntity.class));
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
    public PageResult<User> findAll(SearchCriteria searchCriteria) {
        return userJpaSearchAdapter.findAll(searchCriteria, UserEntityMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsById(String userId) {
        return userJpaRepository.existsById(userId);
    }
}