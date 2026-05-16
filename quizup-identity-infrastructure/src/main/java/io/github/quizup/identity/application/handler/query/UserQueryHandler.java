package io.github.quizup.identity.application.handler.query;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.identity.domain.exception.UserProblems;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import io.github.quizup.identity.domain.query.UserQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

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
    public PageResult<User> handle(UserQuery.UserSearchQuery query) {
        return userReadPort.findAll(query);
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
}