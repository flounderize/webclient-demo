package com.example.webclientdemo.model;

import java.time.OffsetDateTime;
import jakarta.validation.constraints.NotBlank;

/**
 * 用户实体，模拟外部服务返回的用户信息。
 */
public class User {

    @NotBlank
    private String id;

    @NotBlank
    private String name;

    private OffsetDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
