package com.example.webclientdemo.support.exception;

/**
 * 外部服务调用异常，统一封装 WebClient 错误。
 */
public class RemoteServiceException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public RemoteServiceException(int statusCode, String message, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
