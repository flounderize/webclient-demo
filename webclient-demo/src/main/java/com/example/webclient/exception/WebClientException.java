package com.example.webclient.exception;

/**
 * WebClient 自定义异常
 * 
 * @author AI Agent
 * @since 1.0.0
 */
public class WebClientException extends RuntimeException {

    private Integer statusCode;
    private String responseBody;

    public WebClientException(String message) {
        super(message);
    }

    public WebClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebClientException(String message, Integer statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
