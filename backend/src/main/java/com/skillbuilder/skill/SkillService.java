package com.skillbuilder.skill;

import com.skillbuilder.common.NotFoundException;
import com.skillbuilder.courselink.CourseLinkRepository;
import com.skillbuilder.document.DocumentRepository;
import com.skillbuilder.document.StorageService;
import com.skillbuilder.recommendation.RecommendationRepository;
import com.skillbuilder.skill.SkillDtos.SkillRequest;
import com.skillbuilder.skill.SkillDtos.SkillResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class SkillService {

    private final SkillRepository skills;
    private final DocumentRepository documents;
    private final CourseLinkRepository courseLinks;
    private final RecommendationRepository recommendations;
    private final StorageService storage;

    public SkillService(SkillRepository skills,
                        DocumentRepository documents,
                        CourseLinkRepository courseLinks,
                        RecommendationRepository recommendations,
                        StorageService storage) {
        this.skills = skills;
        this.documents = documents;
        this.courseLinks = courseLinks;
        this.recommendations = recommendations;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> list() {
        Map<Long, Long> docCounts = toMap(documents.countGroupedBySkill());
        Map<Long, Long> linkCounts = toMap(courseLinks.countGroupedBySkill());
        Map<Long, Long> recCounts = toMap(recommendations.countGroupedBySkill());
        return skills.findAll().stream()
                .map(s -> SkillResponse.of(s,
                        docCounts.getOrDefault(s.getId(), 0L),
                        linkCounts.getOrDefault(s.getId(), 0L),
                        recCounts.getOrDefault(s.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public SkillResponse get(Long id) {
        return toResponse(find(id));
    }

    public SkillResponse create(SkillRequest request) {
        if (skills.existsByNameIgnoreCase(request.name().trim())) {
            throw new IllegalArgumentException("A skill named '" + request.name().trim() + "' already exists");
        }
        Skill skill = skills.save(new Skill(request.name().trim(), request.description()));
        return toResponse(skill);
    }

    public SkillResponse update(Long id, SkillRequest request) {
        Skill skill = find(id);
        String newName = request.name().trim();
        if (!skill.getName().equalsIgnoreCase(newName) && skills.existsByNameIgnoreCase(newName)) {
            throw new IllegalArgumentException("A skill named '" + newName + "' already exists");
        }
        skill.setName(newName);
        skill.setDescription(request.description());
        return toResponse(skill);
    }

    public void delete(Long id) {
        Skill skill = find(id);
        skills.delete(skill);
        storage.deleteSkillDirectory(id);
    }

    Skill find(Long id) {
        return skills.findById(id).orElseThrow(() -> NotFoundException.of("Skill", id));
    }

    private SkillResponse toResponse(Skill skill) {
        return SkillResponse.of(skill,
                documents.countBySkillId(skill.getId()),
                courseLinks.countBySkillId(skill.getId()),
                recommendations.countBySkillId(skill.getId()));
    }

    private static Map<Long, Long> toMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }
}
