package io.github.quizup.identity.domain.port.out;

import io.github.quizup.identity.domain.model.LoginCode;

import java.util.Optional;

/**
 * Port sortant - Persistance des codes de connexion à usage unique (OTP email).
 *
 * <p>Un seul code actif par email (clé primaire = email) : une nouvelle demande écrase
 * le code précédent.</p>
 */
public interface LoginCodeRepositoryPort {

    /**
     * Persiste (crée ou remplace) le code de connexion pour l'email.
     */
    void save(LoginCode loginCode);

    /**
     * Retourne le code de connexion d'un email, s'il existe.
     */
    Optional<LoginCode> findByEmail(String email);

    /**
     * Supprime le code d'un email.
     */
    void deleteByEmail(String email);
}
