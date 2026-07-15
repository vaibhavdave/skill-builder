package com.skillbuilder.courselink;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CourseLinkRepository extends JpaRepository<CourseLink, Long> {

    List<CourseLink> findBySkillIdOrderByCreatedAtDesc(Long skillId);

    long countBySkillId(Long skillId);

    @Query("select c.skill.id, count(c) from CourseLink c group by c.skill.id")
    List<Object[]> countGroupedBySkill();
}
