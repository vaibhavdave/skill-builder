package com.skillbuilder.document;

import com.skillbuilder.document.DocumentDtos.DocumentResponse;
import com.skillbuilder.document.DocumentDtos.DocumentUpdateRequest;
import com.skillbuilder.document.DocumentService.StoredFile;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @GetMapping("/skills/{skillId}/documents")
    public List<DocumentResponse> list(@PathVariable Long skillId) {
        return service.list(skillId);
    }

    @PostMapping("/skills/{skillId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(@PathVariable Long skillId,
                                   @RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "title", required = false) String title,
                                   @RequestParam(value = "notes", required = false) String notes) {
        return service.upload(skillId, file, title, notes);
    }

    @PutMapping("/documents/{id}")
    public DocumentResponse update(@PathVariable Long id,
                                   @Valid @RequestBody DocumentUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/documents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    /** Opens inline in the browser (PDFs render in the built-in viewer). */
    @GetMapping("/documents/{id}/view")
    public ResponseEntity<Resource> view(@PathVariable Long id) {
        return serve(service.file(id), false);
    }

    /** Forces a download with the original filename. */
    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        return serve(service.file(id), true);
    }

    private ResponseEntity<Resource> serve(StoredFile file, boolean attachment) {
        Document document = file.document();
        MediaType mediaType = document.getContentType() != null
                ? MediaType.parseMediaType(document.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        ContentDisposition disposition = (attachment
                ? ContentDisposition.attachment()
                : ContentDisposition.inline())
                .filename(document.getOriginalFilename())
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.resource());
    }
}
