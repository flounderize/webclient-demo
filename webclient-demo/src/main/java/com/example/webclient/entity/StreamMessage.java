package com.example.webclient.entity;

import java.time.LocalDateTime;

/**
 * 流式消息实体类
 * 
 * @author AI Agent
 * @since 1.0.0
 */
public class StreamMessage {

    private String id;
    private String type;
    private String content;
    private Integer sequence;
    private LocalDateTime timestamp;
    private boolean finished;

    public StreamMessage() {
    }

    public StreamMessage(String id, String content) {
        this.id = id;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    @Override
    public String toString() {
        return "StreamMessage{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", content='" + content + '\'' +
                ", sequence=" + sequence +
                ", timestamp=" + timestamp +
                ", finished=" + finished +
                '}';
    }
}
