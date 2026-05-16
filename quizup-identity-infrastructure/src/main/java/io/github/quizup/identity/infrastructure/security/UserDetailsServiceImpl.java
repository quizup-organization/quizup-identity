package io.github.quizup.identity.infrastructure.security;

import io.github.quizup.identity.domain.model.User;
import io.github.quizup.identity.domain.port.out.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service de recuperation des details utilisateur.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final UserRepositoryPort userRepositoryPort;

    public UserDetailsServiceImpl(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by username: {}", username);

        User user = userRepositoryPort.findByEmail(username)
                .orElseThrow(() ->  new UsernameNotFoundException("User not found: " + username));

        if (!user.hasPassword()) {
            logger.warn("User {} has no password (OAuth2 account)", username);
            throw new UsernameNotFoundException("User has no password (OAuth2 account only): " + username);
        }

        logger.debug("User loaded successfully: userId={}", user.userId());

        return new UserPrincipal(
                user.userId(),
                user.email(),
                user.password()
        );
    }

}
