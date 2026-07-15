package com.skillbuilder.courselink;

import com.skillbuilder.common.NotFoundException;
import com.skillbuilder.courselink.CourseLinkDtos.CourseLinkRequest;
import com.skillbuilder.courselink.CourseLinkDtos.CourseLinkResponse;
import com.skillbuilder.skill.Skill;
import com.skillbuilder.skill.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;

@Service
@Transactional
public class CourseLinkService {

    private final CourseLinkRepository links;
    private final SkillRepository skills;

    public CourseLinkService(CourseLinkRepository links, SkillRepository skills) {
        this.links = links;
        this.skills = skills;
    }

    @Transactional(readOnly = true)
    public List<CourseLinkResponse> list(Long skillId) {
        requireSkill(skillId);
        return links.findBySkillIdOrderByCreatedAtDesc(skillId).stream()
                .map(CourseLinkResponse::of)
                .toList();
    }

    public CourseLinkResponse create(Long skillId, CourseLinkRequest request) {
        Skill skill = requireSkill(skillId);
        CourseLink link = new CourseLink(skill, normalizeUrl(request.url()),
                request.title(), request.provider(), request.notes());
        if (request.status() != null) {
            link.setStatus(request.status());
        }
        return CourseLinkResponse.of(links.save(link));
    }

    public CourseLinkResponse update(Long id, CourseLinkRequest request) {
        CourseLink link = find(id);
        link.setUrl(normalizeUrl(request.url()));
        link.setTitle(request.title());
        link.setProvider(request.provider());
        link.setNotes(request.notes());
        if (request.status() != null) {
            link.setStatus(request.status());
        }
        return CourseLinkResponse.of(link);
    }

    public void delete(Long id) {
        links.delete(find(id));
    }

    private CourseLink find(Long id) {
        return links.findById(id).orElseThrow(() -> NotFoundException.of("Course link", id));
    }

    private Skill requireSkill(Long skillId) {
        return skills.findById(skillId).orElseThrow(() -> NotFoundException.of("Skill", skillId));
    }

    private static String normalizeUrl(String url) {
        String trimmed = url.trim();
        if (!trimmed.matches("(?i)https?://.*")) {
            trimmed = "https://" + trimmed;
        }
        try {
            URI.create(trimmed);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("'" + url + "' is not a valid URL");
        }
        return trimmed;
    }
}
