package io.github.quizup.identity.infrastructure.security;

import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service de recuperation des details utilisateur.
 * Conservé pour les usages de type UserDetails (non utilisé par le flux passwordless,
 * qui construit directement le principal après vérification du code OTP).
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final UserRepositoryPort userRepositoryPort;
    private final Roles roles;

    public UserDetailsServiceImpl(UserRepositoryPort userRepositoryPort, Roles roles) {
        this.userRepositoryPort = userRepositoryPort;
        this.roles = roles;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by username: {}", username);

        User user = userRepositoryPort.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        logger.debug("User loaded successfully: userId={}", user.userId());

        return new UserPrincipal(
                user.userId(),
                user.email(),
                roles.forUser(user.userId(), user.email())
        );
    }
}
