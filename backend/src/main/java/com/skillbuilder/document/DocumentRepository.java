package com.skillbuilder.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findBySkillIdOrderByUploadedAtDesc(Long skillId);

    long countBySkillId(Long skillId);

    @Query("select d.skill.id, count(d) from Document d group by d.skill.id")
    List<Object[]> countGroupedBySkill();
}
