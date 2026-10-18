package io.github.quizup.identity.domain.exception;

import io.github.quizup.microservice.core.domain.exception.BaseProblem;
import io.github.quizup.microservice.core.domain.exception.ProblemCategory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Problèmes métier des signalements de joueurs et de la modération des comptes. */
public final class UserReportProblems {

    private UserReportProblems() {
    }

    public static class UserReportNotFoundProblem extends BaseReportProblem {
        public UserReportNotFoundProblem(String reportId) {
            super(reportId, "urn:quizup:user:reportNotFound",
                    ProblemCategory.BUSINESS_RESOURCE_MISSING,
                    "User report not found",
                    "No user report with id " + reportId);
        }
    }

    public static class InvalidUserReportProblem extends BaseReportProblem {
        public InvalidUserReportProblem(String reportId, String detail) {
            super(reportId, "urn:quizup:user:reportInvalid",
                    ProblemCategory.VALIDATION,
                    "Invalid user report",
                    detail);
        }
    }

    public static class AccountSuspendedProblem extends BaseAccountProblem {
        public AccountSuspendedProblem(String userId) {
            this(userId, null);
        }

        public AccountSuspendedProblem(String userId, Instant suspendedUntil) {
            super(userId, "urn:quizup:identity:accountSuspended",
                    ProblemCategory.PERMISSION,
                    "Account suspended",
                    "The account is suspended and cannot authenticate",
                    Map.of("suspendedUntil", suspendedUntil == null ? "" : suspendedUntil.toString()));
        }
    }

    public static class AlreadySuspendedProblem extends BaseAccountProblem {
        public AlreadySuspendedProblem(String userId) {
            super(userId, "urn:quizup:identity:accountAlreadySuspended",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Account already suspended",
                    "The account is already suspended");
        }
    }

    public static class SystemAccountNotSuspendableProblem extends BaseAccountProblem {
        public SystemAccountNotSuspendableProblem(String userId) {
            super(userId, "urn:quizup:identity:systemAccountProtected",
                    ProblemCategory.PERMISSION,
                    "Protected account",
                    "The system account cannot be suspended");
        }
    }

    public static class InvalidSuspensionProblem extends BaseAccountProblem {
        public InvalidSuspensionProblem(String userId, String detail) {
            super(userId, "urn:quizup:identity:invalidSuspension",
                    ProblemCategory.VALIDATION,
                    "Invalid suspension",
                    detail);
        }
    }

    public abstract static class BaseReportProblem extends BaseProblem {
        protected BaseReportProblem(String reportId,
                                    String type,
                                    ProblemCategory category,
                                    String title,
                                    String detail) {
            super(type, category, title, detail, Map.of("reportId", reportId));
        }
    }

    public abstract static class BaseAccountProblem extends BaseProblem {
        protected BaseAccountProblem(String userId,
                                     String type,
                                     ProblemCategory category,
                                     String title,
                                     String detail) {
            this(userId, type, category, title, detail, Map.of());
        }

        protected BaseAccountProblem(String userId,
                                     String type,
                                     ProblemCategory category,
                                     String title,
                                     String detail,
                                     Map<String, Object> extraContext) {
            super(type, category, title, detail, mergeContext(extraContext, userId));
        }

        private static Map<String, Object> mergeContext(Map<String, Object> extraContext, String userId) {
            Map<String, Object> context = new HashMap<>();
            if (extraContext != null) {
                context.putAll(extraContext);
            }
            context.put("userId", userId);
            return context;
        }
    }
}
