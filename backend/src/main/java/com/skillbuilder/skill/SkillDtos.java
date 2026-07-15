package com.skillbuilder.skill;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public final class SkillDtos {

    private SkillDtos() {
    }

    public record SkillRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 5000) String description) {
    }

    public record SkillResponse(
            Long id,
            String name,
            String description,
            long documentCount,
            long courseLinkCount,
            long recommendationCount,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {

        public static SkillResponse of(Skill skill, long documents, long links, long recommendations) {
            return new SkillResponse(skill.getId(), skill.getName(), skill.getDescription(),
                    documents, links, recommendations, skill.getCreatedAt(), skill.getUpdatedAt());
        }
    }
}
