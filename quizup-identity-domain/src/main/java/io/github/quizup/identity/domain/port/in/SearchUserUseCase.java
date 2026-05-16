package io.github.quizup.identity.domain.port.in;

import io.github.quizup.common.domain.model.search.*;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.query.UserQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SearchUserUseCase {

    CompletableFuture<PageResult<User>> search(UserQuery.UserSearchQuery query);

    default CompletableFuture<PageResult<User>> search(List<FilterCriteria> filters,
                                                       List<SortCriteria> sorts,
                                                       PageCriteria page) {
        return search(
                new UserQuery.UserSearchQuery(
                        filters,
                        sorts,
                        page
                )
        );
    }
}

