package com.example.webclient.dto;

import java.util.List;

/**
 * 推荐请求 DTO
 * 
 * @author AI Agent
 * @since 1.0.0
 */
public class RecommendationRequest {

    private Long userId;
    private String category;
    private Integer limit;
    private List<String> excludeTags;
    private Double minScore;

    public RecommendationRequest() {
    }

    public RecommendationRequest(Long userId, String category, Integer limit) {
        this.userId = userId;
        this.category = category;
        this.limit = limit;
    }

    // Getters and Setters

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public List<String> getExcludeTags() {
        return excludeTags;
    }

    public void setExcludeTags(List<String> excludeTags) {
        this.excludeTags = excludeTags;
    }

    public Double getMinScore() {
        return minScore;
    }

    public void setMinScore(Double minScore) {
        this.minScore = minScore;
    }

    @Override
    public String toString() {
        return "RecommendationRequest{" +
                "userId=" + userId +
                ", category='" + category + '\'' +
                ", limit=" + limit +
                ", excludeTags=" + excludeTags +
                ", minScore=" + minScore +
                '}';
    }
}
