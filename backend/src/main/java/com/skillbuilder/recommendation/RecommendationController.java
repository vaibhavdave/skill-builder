package com.skillbuilder.recommendation;

import com.skillbuilder.recommendation.RecommendationDtos.RecommendationResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/skills/{skillId}/recommendations")
public class RecommendationController {

    private final RecommendationService service;

    public RecommendationController(RecommendationService service) {
        this.service = service;
    }

    @GetMapping
    public List<RecommendationResponse> list(@PathVariable Long skillId) {
        return service.list(skillId);
    }

    @PostMapping("/generate")
    public List<RecommendationResponse> generate(@PathVariable Long skillId) {
        return service.generate(skillId);
    }
}
