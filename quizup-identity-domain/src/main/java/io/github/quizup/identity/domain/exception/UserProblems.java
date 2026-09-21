package io.github.quizup.identity.domain.exception;

import io.github.quizup.microservice.core.domain.exception.ProblemCategory;

import java.util.Map;


public interface UserProblems {

    class UserNotFoundProblem extends UserProblem {
        public UserNotFoundProblem(String userId) {
            super(userId, "urn:quizup:user:notFound",
                    ProblemCategory.BUSINESS_RESOURCE_MISSING,
                    "User not found",
                    "The user " + userId + " was not found", null);
        }
    }

    class InvalidLoginCodeProblem extends UserProblem {
        public InvalidLoginCodeProblem(String email) {
            super("UNKNOWN", "urn:quizup:auth:invalidLoginCode", "Invalid login code",
                    "The login code is invalid, expired or already used",
                    Map.of("email", email));
        }
    }

    class InvalidEmailFormatProblem extends UserProblem {
        public InvalidEmailFormatProblem(String userId, String email) {
            super(userId, "urn:quizup:user:invalidEmailFormat", "Invalid email format",
                    "The email " + email + " has an invalid format",
                    Map.of("email", email));
        }
    }

    class SocialProviderMissingProblem extends UserProblem {
        public SocialProviderMissingProblem(String userId) {
            super(userId, "urn:quizup:user:missingSocialProvider",
                    "Missing social provider",
                    "Provider information is required for social registration");
        }
    }

    class UserAlreadyExistsProblem extends UserProblem {
        public UserAlreadyExistsProblem(String userId, String email) {
            super(userId, "urn:quizup:user:alreadyExists", "User already exists",
                    "A user with email " + email + " already exists");
        }
    }
}
