package io.github.quizup.identity.infrastructure.in.api.mapper;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.infrastructure.in.api.response.PageResponse;
import io.github.quizup.microservice.core.infrastructure.mapper.SearchResponseMapper;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.infrastructure.in.api.response.UserResponse;


public final class UserResponseMapper {
    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.userId(),
                user.email(),
                user.name(),
                user.linkedSocialAccounts(),
                user.createdAt()
        );
    }
    public static PageResponse<UserResponse> toResponse(PageResult<User> pageResult) {
        return SearchResponseMapper.toSearchResponse(pageResult, UserResponseMapper::toResponse);
    }
}

