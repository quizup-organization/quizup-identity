package io.github.quizup.identity.domain.query;

import io.github.quizup.microservice.core.domain.model.search.FilterCriteria;
import io.github.quizup.microservice.core.domain.model.search.PageCriteria;
import io.github.quizup.microservice.core.domain.model.search.SortCriteria;
import io.github.quizup.microservice.core.domain.query.SearchQuery;

import java.util.List;

public interface UserQuery {

    record UserSearchQuery(
            List<FilterCriteria> filters,
            List<SortCriteria> sorts,
            PageCriteria page
    ) implements UserQuery, SearchQuery {
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
