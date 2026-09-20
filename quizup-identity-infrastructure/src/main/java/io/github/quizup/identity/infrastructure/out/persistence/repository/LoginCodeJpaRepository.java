package io.github.quizup.identity.infrastructure.out.persistence.repository;

import io.github.quizup.identity.infrastructure.out.persistence.entity.LoginCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginCodeJpaRepository extends JpaRepository<LoginCodeEntity, String> {
}
