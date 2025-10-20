package com.example.webclient.entity;

import java.time.LocalDateTime;

/**
 * 通知实体类（用于 SSE）
 * 
 * @author AI Agent
 * @since 1.0.0
 */
public class Notification {

    private String id;
    private String userId;
    private String type;
    private String title;
    private String message;
    private String priority;
    private LocalDateTime timestamp;
    private boolean read;

    public Notification() {
    }

    public Notification(String id, String message) {
        this.id = id;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", priority='" + priority + '\'' +
                ", timestamp=" + timestamp +
                ", read=" + read +
                '}';
    }
}
