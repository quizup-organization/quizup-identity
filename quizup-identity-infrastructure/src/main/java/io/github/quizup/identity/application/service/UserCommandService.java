package io.github.quizup.identity.application.service;

import io.github.quizup.identity.domain.port.in.RegisterUserUseCase;
import io.github.quizup.identity.domain.command.UserCommand;
import io.github.quizup.identity.domain.model.SocialProvider;
import io.github.quizup.identity.domain.port.out.PasswordEncoderPort;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service applicatif - Implémente RegisterUserUseCase.
 */
@Service
public class UserCommandService implements RegisterUserUseCase {

    private final CommandGateway commandGateway;

    public UserCommandService(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @Override
    public CompletableFuture<String> registerWithPassword(UserCommand.RegisterUserWithPasswordCommand command) {
        return commandGateway.send(command);
    }

    @Override
    public CompletableFuture<String> registerWithSocial(UserCommand.RegisterUserWithSocialCommand command) {
        return commandGateway.send(command);
    }
}

