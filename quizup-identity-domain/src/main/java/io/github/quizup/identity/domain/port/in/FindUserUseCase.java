package io.github.quizup.identity.domain.port.in;

import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.query.UserQuery;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface FindUserUseCase {

    CompletableFuture<Optional<User>> findById(UserQuery.FindUserQuery query);

    CompletableFuture<Optional<User>> findByEmail(UserQuery.FindUserByEmailQuery query);

    default CompletableFuture<Optional<User>> findByEmail(String email) {
        return findByEmail(new UserQuery.FindUserByEmailQuery(email));
    }

     default CompletableFuture<Optional<User>> findById(String id) {
        return findById(new UserQuery.FindUserQuery(id));
    }
}

