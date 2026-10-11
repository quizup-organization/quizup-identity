package io.github.quizup.identity.domain.port.in;

import io.github.quizup.identity.domain.command.UserCommand;

import java.util.concurrent.CompletableFuture;

/**
 * Port entrant - Cas d'utilisation : enregistrement d'une connexion réussie.
 */
@SuppressWarnings("UnusedReturnValue")
public interface RecordUserLoginUseCase {

    CompletableFuture<Void> recordLogin(UserCommand.RecordUserLoginCommand command);

    default CompletableFuture<Void> recordLogin(String userId) {
        return recordLogin(new UserCommand.RecordUserLoginCommand(userId));
    }
}
