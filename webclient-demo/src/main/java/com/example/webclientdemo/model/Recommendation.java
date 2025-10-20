package com.example.webclientdemo.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 推荐实体，用于演示异步调用。
 */
public class Recommendation {

    @NotBlank
    private String id;

    @NotBlank
    private String content;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
