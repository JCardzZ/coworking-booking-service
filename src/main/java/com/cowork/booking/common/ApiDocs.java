package com.cowork.booking.common;

/** OpenAPI values shared across controllers and error schemas. */
public final class ApiDocs {

    public static final String PROBLEM_JSON = "application/problem+json";

    private ApiDocs() {
    }

    public static final class Tags {
        public static final String SPACES = "Spaces";
        public static final String USERS = "Users";
        public static final String RESERVATIONS = "Reservations";

        private Tags() {
        }
    }

    public static final class Examples {
        public static final String TIMESTAMP = "2026-09-26T10:15:30Z";
        public static final String TRACE_ID = "3f1c9a52-8d4e-4b7a-9c61-2e5f0b7d4a18";
        public static final String API_PATH = "/coworking-service" + AppConstants.Api.BASE_PATH;
        public static final String PROBLEM_TYPE_BASE = "http://localhost:8080/coworking-service" + AppConstants.Problem.ERRORS_PATH;

        private Examples() {
        }
    }
}
