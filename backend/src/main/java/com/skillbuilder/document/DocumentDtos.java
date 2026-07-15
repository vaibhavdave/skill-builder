package com.skillbuilder.document;

import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public final class DocumentDtos {

    private DocumentDtos() {
    }

    public record DocumentUpdateRequest(
            @Size(max = 300) String title,
            @Size(max = 5000) String notes) {
    }

    public record DocumentResponse(
            Long id,
            Long skillId,
            String title,
            String originalFilename,
            String contentType,
            long sizeBytes,
            String notes,
            OffsetDateTime uploadedAt) {

        public static DocumentResponse of(Document d) {
            return new DocumentResponse(d.getId(), d.getSkill().getId(), d.getTitle(),
                    d.getOriginalFilename(), d.getContentType(), d.getSizeBytes(),
                    d.getNotes(), d.getUploadedAt());
        }
    }
}
