package com.example.webclient.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

/**
 * 幂等键与链路追踪生成工具。
 */
public final class IdempotencyKeyGenerator {

    private IdempotencyKeyGenerator() {
    }

    /**
     * 生成全局唯一的幂等键。
     *
     * @return 幂等键字符串
     */
    public static String generate() {
        return generate("idempotency");
    }

    /**
     * 基于前缀生成幂等键。
     *
     * @param prefix 前缀标识
     * @return 幂等键
     */
    public static String generate(String prefix) {
        String raw = prefix + ":" + UUID.randomUUID();
        return base64Sha256(raw);
    }

    /**
     * 生成链路追踪 ID，用于透传。
     *
     * @return traceId
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    private static String base64Sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("无法生成幂等键", ex);
        }
    }
}
