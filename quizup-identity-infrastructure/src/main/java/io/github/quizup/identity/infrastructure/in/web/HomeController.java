package io.github.quizup.identity.infrastructure.in.web;

import io.github.quizup.microservice.core.domain.model.security.QuizUpPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller pour la page d'accueil après authentification
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {

        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof QuizUpPrincipal) {
            model.addAttribute("authenticated", true);
            model.addAttribute("id", ((QuizUpPrincipal) authentication.getPrincipal()).getUserId());
            model.addAttribute("email", ((QuizUpPrincipal) authentication.getPrincipal()).getEmail());
        } else {
            model.addAttribute("authenticated", false);
        }
        return "home";
    }
}
