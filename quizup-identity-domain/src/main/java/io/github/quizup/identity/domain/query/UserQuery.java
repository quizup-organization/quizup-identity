package io.github.quizup.identity.domain.query;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;

public interface UserQuery {

    record UserSearchQuery(SearchRequest request) implements UserQuery {
    }

    record UserExistsByIdQuery(String userId) implements UserQuery {
    }

    record UserExistsByEmailQuery(String email) implements UserQuery {
    }

    record GetUserQuery(String userId) implements UserQuery {
    }

    record FindUserQuery(String userId) implements UserQuery {
    }

    record FindUserByEmailQuery(String email) implements UserQuery {
    }
}
