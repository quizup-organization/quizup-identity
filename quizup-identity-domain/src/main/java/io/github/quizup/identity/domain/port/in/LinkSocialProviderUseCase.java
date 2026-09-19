package io.github.quizup.identity.domain.port.in;

import io.github.quizup.identity.domain.command.UserCommand;
import io.github.quizup.identity.domain.model.SocialProvider;

import java.util.concurrent.CompletableFuture;

/**
 * Port entrant - Cas d'utilisation : liaison d'un provider social à un compte existant.
 */
public interface LinkSocialProviderUseCase {

    CompletableFuture<Void> linkSocialProvider(UserCommand.LinkSocialProviderCommand command);

    default CompletableFuture<Void> linkSocialProvider(String userId, SocialProvider provider) {
        return linkSocialProvider(new UserCommand.LinkSocialProviderCommand(userId, provider));
    }
}
