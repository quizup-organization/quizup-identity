package io.github.quizup.identity.application.service;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.infrastructure.axon.PageResponseTypes;
import io.github.quizup.identity.domain.exception.UserProblems;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.port.in.CheckUserUseCase;
import io.github.quizup.identity.domain.port.in.FindUserUseCase;
import io.github.quizup.identity.domain.port.in.GetUserUseCase;
import io.github.quizup.identity.domain.port.in.SearchUserUseCase;
import io.github.quizup.identity.domain.query.UserQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service applicatif - Implémente GetUserUseCase.
 * Cette classe est autorisée à connaitre QueryGateway (couche application).
 */
@Service
public class UserQueryService implements GetUserUseCase, FindUserUseCase, CheckUserUseCase, SearchUserUseCase {

    private final QueryGateway queryGateway;

    public UserQueryService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public CompletableFuture<Boolean> existsById(UserQuery.UserExistsByIdQuery query) {
        return queryGateway.query(query, ResponseTypes.instanceOf(Boolean.class));
    }

    @Override
    public CompletableFuture<Boolean> existsByEmail(UserQuery.UserExistsByEmailQuery query) {
        return queryGateway.query(query, ResponseTypes.instanceOf(Boolean.class));
    }

    @Override
    public CompletableFuture<Optional<User>> findById(UserQuery.FindUserQuery query) {
        return queryGateway.query(query, ResponseTypes.optionalInstanceOf(User.class));
    }

    @Override
    public CompletableFuture<Optional<User>> findByEmail(UserQuery.FindUserByEmailQuery query) {
        return queryGateway.query(query, ResponseTypes.optionalInstanceOf(User.class));
    }

    @Override
    public CompletableFuture<User> getById(UserQuery.GetUserQuery query) throws UserProblems.UserNotFoundProblem {
        return queryGateway.query(query, ResponseTypes.instanceOf(User.class));
    }

    @Override
    public CompletableFuture<PageResult<User>> search(UserQuery.UserSearchQuery query) {
        return queryGateway.query(query, PageResponseTypes.pageResultOf(User.class));
    }
}

