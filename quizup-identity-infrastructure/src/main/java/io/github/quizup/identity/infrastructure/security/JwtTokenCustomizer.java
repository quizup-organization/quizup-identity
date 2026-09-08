package io.github.quizup.identity.infrastructure.security;

import io.github.quizup.microservice.core.domain.constant.SecurityConstants;
import io.github.quizup.microservice.core.domain.model.security.QuizUpPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Customizer pour ajouter des claims personnalisés aux JWT
 */
@Component
public class JwtTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {
        Authentication authentication = context.getPrincipal();

        if (authentication.getPrincipal() instanceof QuizUpPrincipal quizUpPrincipal) {
            context.getClaims().claims(claims -> {
                // Claims standards
                claims.put("sub", quizUpPrincipal.getUserId());
                claims.put("email", quizUpPrincipal.getEmail());

                // Claims personnalisés QuizUp
                claims.put(SecurityConstants.USER_EMAIL_CLAIM, quizUpPrincipal.getEmail());
                claims.put(SecurityConstants.USER_IDENTIFIER_CLAIM, quizUpPrincipal.getUserId());

                // Rôles
                claims.put("roles", authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()));
            });
        }
    }
}
