package com.skillbuilder.courselink;

import com.skillbuilder.courselink.CourseLinkDtos.CourseLinkRequest;
import com.skillbuilder.courselink.CourseLinkDtos.CourseLinkResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CourseLinkController {

    private final CourseLinkService service;

    public CourseLinkController(CourseLinkService service) {
        this.service = service;
    }

    @GetMapping("/skills/{skillId}/links")
    public List<CourseLinkResponse> list(@PathVariable Long skillId) {
        return service.list(skillId);
    }

    @PostMapping("/skills/{skillId}/links")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseLinkResponse create(@PathVariable Long skillId,
                                     @Valid @RequestBody CourseLinkRequest request) {
        return service.create(skillId, request);
    }

    @PutMapping("/links/{id}")
    public CourseLinkResponse update(@PathVariable Long id,
                                     @Valid @RequestBody CourseLinkRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/links/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
