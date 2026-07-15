package com.skillbuilder.recommendation;

import com.skillbuilder.recommendation.RecommendationDtos.GeneratedResource;
import com.skillbuilder.recommendation.RecommendationDtos.RecommendationResponse;
import com.skillbuilder.skill.Skill;
import com.skillbuilder.skill.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RecommendationRepository recommendations;

    @Mock
    private SkillRepository skills;

    @Mock
    private ClaudeRecommendationClient claude;

    @Mock
    private TransactionTemplate transaction;

    private RecommendationService service;

    private final Skill skill = new Skill("Rust", "Learn systems programming");

    @BeforeEach
    void setUp() {
        service = new RecommendationService(recommendations, skills, claude, transaction);
        lenient().when(skills.findById(1L)).thenReturn(Optional.of(skill));
        // Run the transactional callback inline
        lenient().when(transaction.execute(any())).thenAnswer(inv ->
                inv.<TransactionCallback<Object>>getArgument(0).doInTransaction(null));
        lenient().when(recommendations.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void clampsStarsIntoOneToFiveRange() {
        when(claude.recommendResources(any(), any())).thenReturn(List.of(
                new GeneratedResource("BOOK", "Great Book", "Author", "why", 9, "great book author"),
                new GeneratedResource("COURSE", "Weak Course", "Someone", "why", 0, "weak course")));

        List<RecommendationResponse> result = service.generate(1L);

        assertThat(result).extracting(RecommendationResponse::stars).containsExactly(5, 1);
    }

    @Test
    void unknownResourceTypeFallsBackToCourse() {
        when(claude.recommendResources(any(), any())).thenReturn(List.of(
                new GeneratedResource("PODCAST", "Some Show", "Host", "why", 4, "some show")));

        List<RecommendationResponse> result = service.generate(1L);

        assertThat(result).singleElement()
                .extracting(RecommendationResponse::resourceType)
                .isEqualTo(Recommendation.ResourceType.COURSE);
    }

    @Test
    void replacesPreviousRecommendationSet() {
        when(claude.recommendResources(any(), any())).thenReturn(List.of(
                new GeneratedResource("YOUTUBE_VIDEO", "Video", "Channel", "why", 3, "video channel")));

        service.generate(1L);

        org.mockito.Mockito.verify(recommendations).deleteBySkillId(1L);
    }
}
