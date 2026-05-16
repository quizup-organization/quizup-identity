package io.github.quizup.identity.infrastructure.in.api.response;


import io.github.quizup.identity.domain.model.SocialProvider;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

/**
 * DTO pour le profil utilisateur
 */
public record UserResponse(
    String userId,
    String email,
    Set<SocialProvider> linkedSocialAccounts,
    Instant createdAt
) implements Serializable {
}
