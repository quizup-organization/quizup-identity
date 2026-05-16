package io.github.quizup.identity.domain.command;

import io.github.quizup.identity.domain.model.SocialProvider;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public interface UserCommand {

    String userId();

    record RegisterUserWithPasswordCommand(
            @TargetAggregateIdentifier String userId,
            String email,
            String password
    ) implements UserCommand {
    }

    record RegisterUserWithSocialCommand(
            @TargetAggregateIdentifier String userId,
            String email,
            SocialProvider provider
    ) implements UserCommand {
    }
}
