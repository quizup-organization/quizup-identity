package io.github.quizup.identity.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Active les balayeurs planifiés (expiration des suspensions temporaires). */
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}
