package com.skillbuilder.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findBySkillIdOrderByResourceTypeAscStarsDesc(Long skillId);

    long countBySkillId(Long skillId);

    @Modifying
    @Query("delete from Recommendation r where r.skill.id = :skillId")
    void deleteBySkillId(Long skillId);

    @Query("select r.skill.id, count(r) from Recommendation r group by r.skill.id")
    List<Object[]> countGroupedBySkill();
}
