package io.github.quizup.identity.domain.port.out;

import io.github.quizup.identity.domain.model.UserSuspensionReason;

import java.time.Duration;
import java.time.Instant;

/**
 * Port sortant d'envoi d'emails transactionnels.
 */
public interface EmailSenderPort {

    /**
     * Envoie un code de connexion OTP.
     *
     * @param email destinataire
     * @param code  code en clair (jamais persisté)
     * @param ttl   durée de validité
     */
    void sendLoginCode(String email, String code, Duration ttl);

    /** Code de confirmation pour la suppression définitive du compte. */
    void sendAccountDeletionCode(String email, String code, Duration ttl);

    /**
     * Informe un joueur de la suspension de son compte.
     *
     * @param email          destinataire
     * @param reason         motif public
     * @param suspendedUntil échéance ({@code null} = définitive)
     */
    void sendAccountSuspended(String email, UserSuspensionReason reason, Instant suspendedUntil);
}
