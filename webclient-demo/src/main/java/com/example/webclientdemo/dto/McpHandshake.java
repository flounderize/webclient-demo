package com.example.webclientdemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP 握手请求，遵循 MCP 规范中的客户端问候语格式。
 */
public class McpHandshake {

    @JsonProperty("version")
    private String version;

    @JsonProperty("capabilities")
    private Object capabilities;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Object getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Object capabilities) {
        this.capabilities = capabilities;
    }
}
