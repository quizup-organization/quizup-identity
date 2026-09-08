package io.github.quizup.identity.infrastructure.in.api;

import io.github.quizup.microservice.core.domain.model.security.QuizUpPrincipal;
import io.github.quizup.microservice.security.SecurityHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AuthenticationController - API REST pour l'authentification
 */
@RestController
@RequestMapping("/api/authentication")
public class AuthenticationController {


    /**
     * Récupérer l'authentification depuis le token
     */
    @GetMapping("/me")
    public ResponseEntity<QuizUpPrincipal> me() {
        return ResponseEntity.ok(SecurityHelper.getPrincipal());
    }
}
