package com.example.webclientdemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP SSE 事件包装，包含 event 与 data 字段。
 */
public class McpEvent {

    @JsonProperty("event")
    private String event;

    @JsonProperty("data")
    private McpMessage data;

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public McpMessage getData() {
        return data;
    }

    public void setData(McpMessage data) {
        this.data = data;
    }
}
