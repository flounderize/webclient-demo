package com.example.webclientdemo.model;

import java.time.OffsetDateTime;

/**
 * 流式消息实体，配合 Flux 调用演示。
 */
public class StreamMessage {

    private String id;
    private String type;
    private String data;
    private OffsetDateTime timestamp;

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

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
