package io.github.quizup.identity.infrastructure.out.persistence.adapter;

import io.github.quizup.identity.domain.model.LoginCode;
import io.github.quizup.identity.domain.port.out.LoginCodeRepositoryPort;
import io.github.quizup.identity.infrastructure.out.persistence.entity.LoginCodeEntity;
import io.github.quizup.identity.infrastructure.out.persistence.repository.LoginCodeJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LoginCodeRepositoryAdapter implements LoginCodeRepositoryPort {

    private final LoginCodeJpaRepository repository;

    public LoginCodeRepositoryAdapter(LoginCodeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(LoginCode loginCode) {
        repository.save(toEntity(loginCode));
    }

    @Override
    public Optional<LoginCode> findByEmail(String email) {
        return repository.findById(email).map(LoginCodeRepositoryAdapter::toDomain);
    }

    @Override
    public void deleteByEmail(String email) {
        repository.deleteById(email);
    }

    private static LoginCodeEntity toEntity(LoginCode code) {
        LoginCodeEntity entity = new LoginCodeEntity();
        entity.setEmail(code.email());
        entity.setCodeHash(code.codeHash());
        entity.setAttempts(code.attempts());
        entity.setExpiresAt(code.expiresAt());
        entity.setCreatedAt(code.createdAt());
        entity.setConsumedAt(code.consumedAt());
        return entity;
    }

    private static LoginCode toDomain(LoginCodeEntity entity) {
        return new LoginCode(
                entity.getEmail(),
                entity.getCodeHash(),
                entity.getAttempts(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getConsumedAt()
        );
    }
}
