package com.skillbuilder.recommendation;

import com.skillbuilder.skill.Skill;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "recommendation")
public class Recommendation {

    public enum ResourceType {
        BOOK, COURSE, YOUTUBE_VIDEO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 20)
    private ResourceType resourceType;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 200)
    private String creator;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(nullable = false)
    private short stars;

    @Column(name = "search_query", length = 300)
    private String searchQuery;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    protected Recommendation() {
    }

    public Recommendation(Skill skill, ResourceType resourceType, String title, String creator,
                          String reason, short stars, String searchQuery) {
        this.skill = skill;
        this.resourceType = resourceType;
        this.title = title;
        this.creator = creator;
        this.reason = reason;
        this.stars = stars;
        this.searchQuery = searchQuery;
    }

    @PrePersist
    void onCreate() {
        generatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Skill getSkill() {
        return skill;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public String getTitle() {
        return title;
    }

    public String getCreator() {
        return creator;
    }

    public String getReason() {
        return reason;
    }

    public short getStars() {
        return stars;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }
}
