package com.example.webclient.config;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * WebClient 属性配置，支持多外部服务自定义参数。
 */
@ConfigurationProperties(prefix = "app.webclient")
public class WebClientProperties {

    /**
     * 默认外部服务参数。
     */
    @NestedConfigurationProperty
    private final ServiceProperties defaults = new ServiceProperties();

    /**
     * 针对不同服务的个性化配置。
     */
    private Map<String, ServiceProperties> services;

    public ServiceProperties getDefaults() {
        return defaults;
    }

    public Map<String, ServiceProperties> getServices() {
        return services;
    }

    public void setServices(Map<String, ServiceProperties> services) {
        this.services = services;
    }

    /**
     * 单个服务的配置项定义。
     */
    public static class ServiceProperties {

        private String baseUrl;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(30);
        private Duration writeTimeout = Duration.ofSeconds(30);
        private Integer maxInMemorySize = 16 * 1024 * 1024;
        private RetryProperties retry = new RetryProperties();
        private AuthProperties auth = new AuthProperties();

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public Duration getWriteTimeout() {
            return writeTimeout;
        }

        public void setWriteTimeout(Duration writeTimeout) {
            this.writeTimeout = writeTimeout;
        }

        public Integer getMaxInMemorySize() {
            return maxInMemorySize;
        }

        public void setMaxInMemorySize(Integer maxInMemorySize) {
            this.maxInMemorySize = maxInMemorySize;
        }

        public RetryProperties getRetry() {
            return retry;
        }

        public void setRetry(RetryProperties retry) {
            this.retry = retry;
        }

        public AuthProperties getAuth() {
            return auth;
        }

        public void setAuth(AuthProperties auth) {
            this.auth = auth;
        }
    }

    /**
     * 重试策略属性。
     */
    public static class RetryProperties {
        private boolean enabled = true;
        private int maxAttempts = 3;
        private Duration firstBackoff = Duration.ofMillis(200);
        private Duration maxBackoff = Duration.ofSeconds(2);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getFirstBackoff() {
            return firstBackoff;
        }

        public void setFirstBackoff(Duration firstBackoff) {
            this.firstBackoff = firstBackoff;
        }

        public Duration getMaxBackoff() {
            return maxBackoff;
        }

        public void setMaxBackoff(Duration maxBackoff) {
            this.maxBackoff = maxBackoff;
        }
    }

    /**
     * 鉴权配置。
     */
    public static class AuthProperties {
        private String type = "none"; // none | bearer | api-key | hmac
        private String token;
        private String apiKeyHeader = "X-API-Key";
        private String apiKeyValue;
        private String hmacSecret;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getApiKeyHeader() {
            return apiKeyHeader;
        }

        public void setApiKeyHeader(String apiKeyHeader) {
            this.apiKeyHeader = apiKeyHeader;
        }

        public String getApiKeyValue() {
            return apiKeyValue;
        }

        public void setApiKeyValue(String apiKeyValue) {
            this.apiKeyValue = apiKeyValue;
        }

        public String getHmacSecret() {
            return hmacSecret;
        }

        public void setHmacSecret(String hmacSecret) {
            this.hmacSecret = hmacSecret;
        }
    }
}
