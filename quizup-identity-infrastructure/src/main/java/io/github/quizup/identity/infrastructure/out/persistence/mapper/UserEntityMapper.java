package io.github.quizup.identity.infrastructure.out.persistence.mapper;

import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.infrastructure.out.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;

/**
 * Mapper infrastructure - Convertit entre UserEntity (JPA) et User (domaine).
 * Seule classe autorisée à connaitre les deux types simultanément.
 */
public final class UserEntityMapper {

    /**
     * Convertit une entité JPA en modèle domaine.
     * Ne retourne jamais null - lance NullPointerException si entity est null.
     */
    public static User toDomain(UserEntity entity) {
        return new User(
                entity.getUserId(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getLinkedSocialAccounts(),
                entity.getCreatedAt()
        );
    }

    /**
     * Convertit un modèle domaine en entité JPA.
     * Utilise lors de la persistance initiale.
     */
    public static UserEntity toEntity(User user) {
        UserEntity userEntity = new UserEntity();
        userEntity.setUserId(user.userId());
        userEntity.setEmail(user.email());
        userEntity.setPassword(user.password());
        userEntity.setLinkedSocialAccounts(user.linkedSocialAccounts());
        userEntity.setCreatedAt(user.createdAt());
        return userEntity;
    }
}

