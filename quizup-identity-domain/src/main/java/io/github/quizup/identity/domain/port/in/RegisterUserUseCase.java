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
     * Enregistre un nouvel utilisateur sans credential (passwordless / compte système).
     */
    CompletableFuture<String> registerUser(UserCommand.RegisterUserCommand command);

    /**
     * Enregistre un nouvel utilisateur via un provider social (OAuth2).
     */
    CompletableFuture<String> registerWithSocial(UserCommand.RegisterUserWithSocialCommand command);

    default CompletableFuture<String> registerUser(String userId, String email) {
        return registerUser(new UserCommand.RegisterUserCommand(userId, email));
    }

    default CompletableFuture<String> registerWithSocial(String userId, String email, SocialProvider socialProvider) {
        return registerWithSocial(
                new UserCommand.RegisterUserWithSocialCommand(
                        userId,
                        email,
                        socialProvider
                )
        );
    }
}
