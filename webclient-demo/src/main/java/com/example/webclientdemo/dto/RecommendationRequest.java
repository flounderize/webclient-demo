package com.example.webclientdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 推荐请求 DTO，演示 JSON 请求体。
 */
public class RecommendationRequest {

    @NotBlank
    private String userId;

    @NotNull
    private Map<String, Object> context;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
}
