package io.github.quizup.identity.domain.port.in;

import io.github.quizup.identity.domain.exception.UserProblems;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.query.UserQuery;

import java.util.concurrent.CompletableFuture;

/**
 * Port entrant - Cas d'utilisation : recuperation d'un utilisateur par ID.
 * Implémente par UserQueryService dans application/service/
 */
public interface GetUserUseCase {

    /**
     * Récupère un utilisateur par son identifiant.
     *
     * @param query query contenant l'identifiant de l'utilisateur
     * @return un CompletableFuture contenant un utilisateur
     * @throws UserProblems.UserNotFoundProblem si l'utilisateur n'existe pas
     */
    CompletableFuture<User> getById(UserQuery.GetUserQuery query) throws UserProblems.UserNotFoundProblem;

    default CompletableFuture<User> getById(String id) throws UserProblems.UserNotFoundProblem {
        return getById(new UserQuery.GetUserQuery(id));
    }
}

