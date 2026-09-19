package io.github.quizup.identity.domain.port.in;

import io.github.quizup.identity.domain.command.UserCommand;
import io.github.quizup.identity.domain.model.SocialProvider;

import java.util.concurrent.CompletableFuture;

/**
 * Port entrant - Cas d'utilisation : enregistrement d'un utilisateur.
 */
@SuppressWarnings("UnusedReturnValue")
public interface RegisterUserUseCase {

    /**
     * Enregistre un nouvel utilisateur avec email et mot de passe.
     */
    CompletableFuture<String> registerWithPassword(UserCommand.RegisterUserWithPasswordCommand command);

    /**
     * Enregistre un nouvel utilisateur via un provider social (OAuth2).
     */
    CompletableFuture<String> registerWithSocial(UserCommand.RegisterUserWithSocialCommand command);

    default CompletableFuture<String> registerWithSocial(String userId, String email, SocialProvider socialProvider) {
        return registerWithSocial(
                new UserCommand.RegisterUserWithSocialCommand(
                        userId,
                        email,
                        socialProvider
                )
        );
    }

    default CompletableFuture<String> registerWithPassword(String userId, String email, String password) {
        return registerWithPassword(
                new UserCommand.RegisterUserWithPasswordCommand(
                        userId,
                        email,
                        password
                )
        );
    }
}

