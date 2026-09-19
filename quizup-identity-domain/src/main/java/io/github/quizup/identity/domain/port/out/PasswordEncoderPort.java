package io.github.quizup.identity.domain.port.out;

public interface PasswordEncoderPort {
    String encode(String password);
}
