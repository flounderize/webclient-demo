package com.example.webclient.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP 消息基类
 * 
 * <p>Model Context Protocol (MCP) 消息结构
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpMessage {

    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    @JsonProperty("id")
    private String id;

    @JsonProperty("method")
    private String method;

    @JsonProperty("params")
    private Object params;

    @JsonProperty("result")
    private Object result;

    @JsonProperty("error")
    private McpError error;

    public McpMessage() {
    }

    public McpMessage(String id, String method, Object params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }

    // Getters and Setters

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public McpError getError() {
        return error;
    }

    public void setError(McpError error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "McpMessage{" +
                "jsonrpc='" + jsonrpc + '\'' +
                ", id='" + id + '\'' +
                ", method='" + method + '\'' +
                ", params=" + params +
                ", result=" + result +
                ", error=" + error +
                '}';
    }

    /**
     * MCP 错误对象
     */
    public static class McpError {
        private Integer code;
        private String message;
        private Object data;

        public McpError() {
        }

        public McpError(Integer code, String message) {
            this.code = code;
            this.message = message;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return "McpError{" +
                    "code=" + code +
                    ", message='" + message + '\'' +
                    ", data=" + data +
                    '}';
        }
    }
}
