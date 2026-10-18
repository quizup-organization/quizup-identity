package io.github.quizup.identity.infrastructure.out.persistence.mapper;

import io.github.quizup.identity.domain.model.AccountStatus;
import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.model.UserSuspension;
import io.github.quizup.identity.infrastructure.out.persistence.entity.UserEntity;

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
        UserSuspension suspension = entity.getSuspensionId() == null
                ? null
                : new UserSuspension(
                        entity.getSuspensionId(),
                        entity.getSuspensionReason(),
                        entity.getSuspensionNote(),
                        entity.getSuspendedBy(),
                        entity.getSuspendedAt(),
                        entity.getSuspendedUntil(),
                        entity.getReinstatedAt(),
                        entity.getReinstatedBy(),
                        entity.getReinstatementReason(),
                        entity.getReinstatementNote());

        return new User(
                entity.getUserId(),
                entity.getEmail(),
                entity.getLinkedSocialAccounts(),
                entity.getCreatedAt(),
                entity.getStatus() == null ? AccountStatus.ACTIVE : entity.getStatus(),
                suspension
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
        userEntity.setLinkedSocialAccounts(user.linkedSocialAccounts());
        userEntity.setCreatedAt(user.createdAt());
        userEntity.setStatus(user.status());

        UserSuspension suspension = user.suspension();
        if (suspension != null) {
            userEntity.setSuspensionId(suspension.suspensionId());
            userEntity.setSuspensionReason(suspension.reason());
            userEntity.setSuspensionNote(suspension.note());
            userEntity.setSuspendedBy(suspension.suspendedBy());
            userEntity.setSuspendedAt(suspension.suspendedAt());
            userEntity.setSuspendedUntil(suspension.suspendedUntil());
            userEntity.setReinstatedAt(suspension.reinstatedAt());
            userEntity.setReinstatedBy(suspension.reinstatedBy());
            userEntity.setReinstatementReason(suspension.reinstatementReason());
            userEntity.setReinstatementNote(suspension.reinstatementNote());
        }
        return userEntity;
    }
}
