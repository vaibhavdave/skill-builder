package com.skillbuilder.document;

import com.skillbuilder.common.NotFoundException;
import com.skillbuilder.document.DocumentDtos.DocumentResponse;
import com.skillbuilder.document.DocumentDtos.DocumentUpdateRequest;
import com.skillbuilder.skill.Skill;
import com.skillbuilder.skill.SkillRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documents;
    private final SkillRepository skills;
    private final StorageService storage;

    public DocumentService(DocumentRepository documents, SkillRepository skills, StorageService storage) {
        this.documents = documents;
        this.skills = skills;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(Long skillId) {
        requireSkill(skillId);
        return documents.findBySkillIdOrderByUploadedAtDesc(skillId).stream()
                .map(DocumentResponse::of)
                .toList();
    }

    public DocumentResponse upload(Long skillId, MultipartFile file, String title, String notes) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file was uploaded");
        }
        Skill skill = requireSkill(skillId);
        String storedPath = storage.store(skillId, file);
        String effectiveTitle = StringUtils.hasText(title) ? title.trim() : stripExtension(file);
        Document document = documents.save(new Document(skill, effectiveTitle,
                file.getOriginalFilename(), storedPath, file.getContentType(),
                file.getSize(), notes));
        return DocumentResponse.of(document);
    }

    public DocumentResponse update(Long id, DocumentUpdateRequest request) {
        Document document = find(id);
        if (request.title() != null) {
            document.setTitle(request.title().trim());
        }
        if (request.notes() != null) {
            document.setNotes(request.notes());
        }
        return DocumentResponse.of(document);
    }

    public void delete(Long id) {
        Document document = find(id);
        String storedPath = document.getStoredPath();
        documents.delete(document);
        storage.delete(storedPath);
    }

    @Transactional(readOnly = true)
    public StoredFile file(Long id) {
        Document document = find(id);
        Resource resource = storage.load(document.getStoredPath());
        return new StoredFile(document, resource);
    }

    private Document find(Long id) {
        return documents.findById(id).orElseThrow(() -> NotFoundException.of("Document", id));
    }

    private Skill requireSkill(Long skillId) {
        return skills.findById(skillId).orElseThrow(() -> NotFoundException.of("Skill", skillId));
    }

    private static String stripExtension(MultipartFile file) {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "Untitled";
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    public record StoredFile(Document document, Resource resource) {
    }
}
