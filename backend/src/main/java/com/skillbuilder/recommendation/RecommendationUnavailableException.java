package com.skillbuilder.recommendation;

public class RecommendationUnavailableException extends RuntimeException {

    public RecommendationUnavailableException(String message) {
        super(message);
    }

    public RecommendationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
