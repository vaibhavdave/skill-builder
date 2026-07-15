package com.skillbuilder.courselink;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public final class CourseLinkDtos {

    private CourseLinkDtos() {
    }

    public record CourseLinkRequest(
            @NotBlank @Size(max = 1000) String url,
            @Size(max = 300) String title,
            @Size(max = 100) String provider,
            @Size(max = 5000) String notes,
            CourseLink.Status status) {
    }

    public record CourseLinkResponse(
            Long id,
            Long skillId,
            String url,
            String title,
            String provider,
            String notes,
            CourseLink.Status status,
            OffsetDateTime createdAt) {

        public static CourseLinkResponse of(CourseLink link) {
            return new CourseLinkResponse(link.getId(), link.getSkill().getId(), link.getUrl(),
                    link.getTitle(), link.getProvider(), link.getNotes(), link.getStatus(),
                    link.getCreatedAt());
        }
    }
}
