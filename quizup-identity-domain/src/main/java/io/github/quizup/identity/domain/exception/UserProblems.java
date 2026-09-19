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

    class UserWithEmailNotFoundProblem extends UserProblem {
        public UserWithEmailNotFoundProblem(String userEmail) {
            super("UNKNOW", "urn:quizup:user:notFound",
                    ProblemCategory.BUSINESS_RESOURCE_MISSING,
                    "User not found",
                    "The user with email " + userEmail + " was not found", null);
        }
    }

    class InvalidPasswordFormatProblem extends UserProblem {
        public InvalidPasswordFormatProblem(String userId, String email, String password) {
            super(userId, "urn:quizup:user:invalidPasswordFormat", "Invalid password format",
                    "The password " + password + " has an invalid format",
                    Map.of("email", email, "password", password));
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
