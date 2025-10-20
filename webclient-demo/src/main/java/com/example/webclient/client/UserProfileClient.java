package com.example.webclient.client;

import com.example.webclient.dto.ApiResponse;
import com.example.webclient.entity.User;
import com.example.webclient.exception.WebClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 用户信息客户端（同步调用示例）
 * 
 * <p>演示如何使用 WebClient 进行同步调用：
 * <ul>
 *   <li>使用 Mono.block() 阻塞获取结果</li>
 *   <li>超时控制</li>
 *   <li>错误处理</li>
 *   <li>重试策略</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@Component
public class UserProfileClient {

    private static final Logger log = LoggerFactory.getLogger(UserProfileClient.class);

    private final WebClient webClient;

    public UserProfileClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8080").build();
    }

    /**
     * 同步获取用户信息
     * 
     * <p>使用场景：在必须阻塞等待结果的业务流程中，例如 AI Agent 需要获取用户配置后才能继续执行
     * 
     * @param userId 用户ID
     * @return 用户信息
     * @throws WebClientException 当请求失败时抛出
     */
    public User getProfileSync(String userId) {
        log.info("Fetching user profile synchronously for userId: {}", userId);

        try {
            // 构建请求并阻塞等待结果
            ApiResponse<User> response = webClient.get()
                    .uri("/api/users/{id}", userId)
                    .retrieve()
                    // 处理 4xx 和 5xx 错误
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Error response: {}", errorBody);
                                        return Mono.error(new WebClientException(
                                                "Failed to get user profile: " + errorBody));
                                    })
                    )
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<User>>() {})
                    // 设置超时时间
                    .timeout(Duration.ofSeconds(5))
                    // 重试策略：最多重试 2 次
                    .retry(2)
                    // 阻塞等待结果
                    .block();

            if (response == null || response.getData() == null) {
                throw new WebClientException("User not found for userId: " + userId);
            }

            log.info("Successfully fetched user profile for userId: {}", userId);
            return response.getData();

        } catch (WebClientResponseException e) {
            log.error("WebClient error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new WebClientException("Failed to get user profile", e);
        } catch (Exception e) {
            log.error("Error fetching user profile for userId: {}", userId, e);
            throw new WebClientException("Failed to get user profile", e);
        }
    }

    /**
     * 同步创建用户
     * 
     * @param user 用户信息
     * @return 创建后的用户信息
     */
    public User createUserSync(User user) {
        log.info("Creating user synchronously: {}", user.getUsername());

        try {
            ApiResponse<User> response = webClient.post()
                    .uri("/api/users")
                    .bodyValue(user)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(
                                            new WebClientException("Failed to create user: " + errorBody)))
                    )
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<User>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null || response.getData() == null) {
                throw new WebClientException("Failed to create user");
            }

            log.info("Successfully created user with id: {}", response.getData().getId());
            return response.getData();

        } catch (Exception e) {
            log.error("Error creating user: {}", user.getUsername(), e);
            throw new WebClientException("Failed to create user", e);
        }
    }

    /**
     * 同步更新用户信息
     * 
     * @param userId 用户ID
     * @param user 更新的用户信息
     * @return 更新后的用户信息
     */
    public User updateUserSync(String userId, User user) {
        log.info("Updating user synchronously: {}", userId);

        try {
            ApiResponse<User> response = webClient.put()
                    .uri("/api/users/{id}", userId)
                    .bodyValue(user)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(
                                            new WebClientException("Failed to update user: " + errorBody)))
                    )
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<User>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null || response.getData() == null) {
                throw new WebClientException("Failed to update user");
            }

            log.info("Successfully updated user: {}", userId);
            return response.getData();

        } catch (Exception e) {
            log.error("Error updating user: {}", userId, e);
            throw new WebClientException("Failed to update user", e);
        }
    }

    /**
     * 同步删除用户
     * 
     * @param userId 用户ID
     */
    public void deleteUserSync(String userId) {
        log.info("Deleting user synchronously: {}", userId);

        try {
            webClient.delete()
                    .uri("/api/users/{id}", userId)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(
                                            new WebClientException("Failed to delete user: " + errorBody)))
                    )
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            log.info("Successfully deleted user: {}", userId);

        } catch (Exception e) {
            log.error("Error deleting user: {}", userId, e);
            throw new WebClientException("Failed to delete user", e);
        }
    }
}
