package com.skillbuilder.recommendation;

import java.time.OffsetDateTime;
import java.util.List;

public final class RecommendationDtos {

    private RecommendationDtos() {
    }

    /** What Claude returns for a single resource (parsed from structured output). */
    public record GeneratedResource(
            String type,
            String title,
            String creator,
            String reason,
            int stars,
            String searchQuery) {
    }

    public record GeneratedResources(List<GeneratedResource> resources) {
    }

    public record RecommendationResponse(
            Long id,
            Recommendation.ResourceType resourceType,
            String title,
            String creator,
            String reason,
            int stars,
            String searchQuery,
            OffsetDateTime generatedAt) {

        public static RecommendationResponse of(Recommendation r) {
            return new RecommendationResponse(r.getId(), r.getResourceType(), r.getTitle(),
                    r.getCreator(), r.getReason(), r.getStars(), r.getSearchQuery(),
                    r.getGeneratedAt());
        }
    }
}
