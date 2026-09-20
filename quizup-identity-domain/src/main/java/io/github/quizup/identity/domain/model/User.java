package io.github.quizup.identity.domain.model;

import java.time.Instant;
import java.util.Set;

/**
 * Modèle domaine représentant un utilisateur tel que vu par le domaine.
 * N'importe AUCUN type JPA, Spring, ou framework.
 * Immuable par convention (record).
 *
 * <p>Aucun mot de passe : l'authentification est passwordless (OTP email) ou sociale.</p>
 */
public record User(
        String userId,
        String email,
        Set<SocialProvider> linkedSocialAccounts,
        Instant createdAt
) {
}
