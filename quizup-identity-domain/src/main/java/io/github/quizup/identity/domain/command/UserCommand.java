package io.github.quizup.identity.domain.command;

import io.github.quizup.identity.domain.model.SocialProvider;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public interface UserCommand {

    String userId();

    /**
     * Enregistre un utilisateur sans credential (passwordless / compte système).
     * L'authentification se fait ensuite par code à usage unique (OTP) ou provider social.
     */
    record RegisterUserCommand(
            @TargetAggregateIdentifier String userId,
            String email
    ) implements UserCommand {
    }

    record RegisterUserWithSocialCommand(
            @TargetAggregateIdentifier String userId,
            String email,
            SocialProvider provider
    ) implements UserCommand {
    }

    record LinkSocialProviderCommand(
            @TargetAggregateIdentifier String userId,
            SocialProvider provider
    ) implements UserCommand {
    }
}
