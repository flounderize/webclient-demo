package com.example.webclientdemo.model;

/**
 * 标准错误响应结构，帮助统一外部服务错误格式。
 */
public class ErrorResponse {

    private String code;
    private String message;
    private String traceId;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
