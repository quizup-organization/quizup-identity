package io.github.quizup.identity.infrastructure.out.persistence.adapter;

import io.github.quizup.identity.domain.port.out.EmailClaimPort;
import io.github.quizup.identity.infrastructure.out.persistence.entity.EmailClaimEntity;
import io.github.quizup.identity.infrastructure.out.persistence.repository.EmailClaimJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;

/**
 * Adapter de la réservation d'email : insert immédiat ({@code saveAndFlush}) pour que la
 * violation de la contrainte unique soit détectée dans l'appel, et non au commit.
 */
@Component
public class EmailClaimAdapter implements EmailClaimPort {

    private final EmailClaimJpaRepository repository;

    public EmailClaimAdapter(EmailClaimJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean claim(String email, String userId) {
        String normalized = normalize(email);
        if (normalized == null) {
            return false;
        }
        try {
            repository.saveAndFlush(new EmailClaimEntity(normalized, userId, Instant.now()));
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }

    private String normalize(String email) {
        if (email == null) {
            return null;
        }
        String value = email.trim().toLowerCase(Locale.ROOT);
        return value.isEmpty() ? null : value;
    }
}
