package io.github.quizup.identity.domain.port.out;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.identity.domain.model.User;

import java.util.Optional;

/**
 * Port sortant - Lecture des utilisateurs depuis la persistence.
 */
public interface UserRepositoryPort {

    /**
     * Persiste un utilisateur (creation ou mise a jour).
     *
     * @param user le modèle domaine à persister
     */
    void save(User user);

    /**
     * Trouve un utilisateur par son identifiant.
     *
     * @param userId l'identifiant de l'utilisateur
     * @return l'utilisateur trouve, ou Optional.empty() s'il n'existe pas
     */
    Optional<User> findById(String userId);

    /**
     * Trouve un utilisateur par son email.
     *
     * @param email l'email de l'utilisateur
     * @return l'utilisateur trouve, ou Optional.empty() s'il n'existe pas
     */
    Optional<User> findByEmail(String email);


    PageResult<User> findAll(SearchCriteria searchCriteria);

    /**
     * Vérifie si un utilisateur existe avec cet email.
     *
     * @param email l'email a verifier (non null, non vide)
     * @return true si un utilisateur avec cet email existe deja
     */
    boolean existsByEmail(String email);

    /**
     * Vérifie si un utilisateur existe avec cet identifiant.
     *
     * @param userId l'identifiant a verifier (non null, non vide)
     * @return true si l'utilisateur existe
     */
    boolean existsById(String userId);
}

