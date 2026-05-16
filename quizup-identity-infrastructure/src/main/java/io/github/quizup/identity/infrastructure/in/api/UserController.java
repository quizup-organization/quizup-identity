package io.github.quizup.identity.infrastructure.in.api;

import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.common.infrastructure.in.api.response.PageResponse;
import io.github.quizup.common.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.identity.domain.port.in.GetUserUseCase;
import io.github.quizup.identity.domain.port.in.SearchUserUseCase;
import io.github.quizup.identity.infrastructure.in.api.mapper.UserResponseMapper;
import io.github.quizup.identity.infrastructure.in.api.response.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * UserController - API REST pour les utilisateurs
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final GetUserUseCase getUserUseCase;
    private final SearchUserUseCase searchUserUseCase;

    public UserController(GetUserUseCase getUserUseCase, SearchUserUseCase searchUserUseCase) {
        this.searchUserUseCase = searchUserUseCase;
        this.getUserUseCase = getUserUseCase;
    }

    /**
     * Search users with pagination and sorting
     */
    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<PageResponse<UserResponse>>> search(@RequestBody SearchRequest searchRequest) {
        SearchCriteria searchCriteria = SearchRequestMapper.toSearchCriteria(searchRequest);
        return searchUserUseCase
                .search(
                        searchCriteria.filters(),
                        searchCriteria.sorts(),
                        searchCriteria.page()
                )
                .thenApply(UserResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Récupérer un user par son ID
     */
    @GetMapping("/{userId}")
    public CompletableFuture<ResponseEntity<UserResponse>> getUserById(@PathVariable String userId) {
        return getUserUseCase.getById(userId)
                .thenApply(UserResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }
}
