package com.skillbuilder.recommendation;

import com.skillbuilder.common.NotFoundException;
import com.skillbuilder.recommendation.RecommendationDtos.GeneratedResource;
import com.skillbuilder.recommendation.RecommendationDtos.RecommendationResponse;
import com.skillbuilder.skill.Skill;
import com.skillbuilder.skill.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Locale;

@Service
public class RecommendationService {

    private final RecommendationRepository recommendations;
    private final SkillRepository skills;
    private final ClaudeRecommendationClient claude;
    private final TransactionTemplate transaction;

    public RecommendationService(RecommendationRepository recommendations,
                                 SkillRepository skills,
                                 ClaudeRecommendationClient claude,
                                 TransactionTemplate transaction) {
        this.recommendations = recommendations;
        this.skills = skills;
        this.claude = claude;
        this.transaction = transaction;
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> list(Long skillId) {
        requireSkill(skillId);
        return recommendations.findBySkillIdOrderByResourceTypeAscStarsDesc(skillId).stream()
                .map(RecommendationResponse::of)
                .toList();
    }

    /**
     * Calls Claude outside any transaction (the call can take tens of seconds),
     * then swaps the skill's stored recommendation set in a short transaction.
     */
    public List<RecommendationResponse> generate(Long skillId) {
        Skill skill = requireSkill(skillId);
        List<GeneratedResource> generated = claude.recommendResources(skill.getName(), skill.getDescription());
        return transaction.execute(status -> {
            Skill managed = requireSkill(skillId);
            recommendations.deleteBySkillId(skillId);
            List<Recommendation> saved = recommendations.saveAll(generated.stream()
                    .map(g -> toEntity(managed, g))
                    .toList());
            return saved.stream().map(RecommendationResponse::of).toList();
        });
    }

    private Recommendation toEntity(Skill skill, GeneratedResource g) {
        Recommendation.ResourceType type;
        try {
            type = Recommendation.ResourceType.valueOf(g.type().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            type = Recommendation.ResourceType.COURSE;
        }
        short stars = (short) Math.max(1, Math.min(5, g.stars()));
        String title = truncate(g.title(), 300);
        String creator = truncate(g.creator(), 200);
        String searchQuery = truncate(g.searchQuery(), 300);
        return new Recommendation(skill, type, title, creator, g.reason(), stars, searchQuery);
    }

    private Skill requireSkill(Long skillId) {
        return skills.findById(skillId).orElseThrow(() -> NotFoundException.of("Skill", skillId));
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
