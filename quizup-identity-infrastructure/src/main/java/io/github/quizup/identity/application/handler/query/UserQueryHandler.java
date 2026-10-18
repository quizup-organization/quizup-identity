package io.github.quizup.identity.application.handler.query;

import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.identity.domain.exception.UserProblems;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.model.UserAccountStatus;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import io.github.quizup.identity.domain.query.UserQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Handler Axon — Point d'entrée des queries sur le bus Axon.
 * Délègue aux ports sortants. Ne contient aucune logique propre.
 * UserQueryService fait la même chose pour les appels locaux (intra-service).
 */
@Component
public class UserQueryHandler {

    private final UserRepositoryPort userReadPort;

    public UserQueryHandler(UserRepositoryPort userReadPort) {
        this.userReadPort = userReadPort;
    }

    @QueryHandler
    public SearchResponse<User> handle(UserQuery.UserSearchQuery query) {
        return userReadPort.findAll(query.request());
    }

    @QueryHandler
    public boolean handle(UserQuery.UserExistsByEmailQuery query) {
        return userReadPort.existsByEmail(query.email());
    }

    @QueryHandler
    public boolean handle(UserQuery.UserExistsByIdQuery query) {
        return userReadPort.existsById(query.userId());
    }

    @QueryHandler
    public User handle(UserQuery.GetUserQuery query) {
        return userReadPort.findById(query.userId())
                .orElseThrow(() -> new UserProblems.UserNotFoundProblem(query.userId()));
    }

    @QueryHandler
    public Optional<User> handle(UserQuery.FindUserQuery query) {
        return userReadPort.findById(query.userId());
    }

    @QueryHandler
    public Optional<User> handle(UserQuery.FindUserByEmailQuery query) {
        return userReadPort.findByEmail(query.email());
    }

    @QueryHandler
    public List<User> handle(UserQuery.GetUsersByIdsQuery query) {
        return userReadPort.findAllByIds(query.userIds());
    }

    @QueryHandler
    public UserAccountStatus handle(UserQuery.GetUserAccountStatusQuery query) {
        User user = userReadPort.findById(query.userId())
                .orElseThrow(() -> new UserProblems.UserNotFoundProblem(query.userId()));
        return new UserAccountStatus(
                user.userId(),
                user.status(),
                user.suspension() == null ? null : user.suspension().suspendedUntil());
    }

    @QueryHandler
    public List<UserAccountStatus> handle(UserQuery.GetUserAccountStatusesQuery query) {
        return userReadPort.findAllByIds(query.userIds()).stream()
                .map(user -> new UserAccountStatus(
                        user.userId(),
                        user.status(),
                        user.suspension() == null ? null : user.suspension().suspendedUntil()))
                .toList();
    }
}
