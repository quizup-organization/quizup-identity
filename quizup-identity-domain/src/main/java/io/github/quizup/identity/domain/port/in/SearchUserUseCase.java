package io.github.quizup.identity.domain.port.in;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.query.UserQuery;

import java.util.concurrent.CompletableFuture;

public interface SearchUserUseCase {

    CompletableFuture<SearchResponse<User>> search(UserQuery.UserSearchQuery query);

    default CompletableFuture<SearchResponse<User>> search(SearchRequest request) {
        return search(new UserQuery.UserSearchQuery(request));
    }
}
