package com.example.webclientdemo.service;

import com.example.webclientdemo.client.ContentStreamClient;
import com.example.webclientdemo.client.McpClient;
import com.example.webclientdemo.client.NotificationClient;
import com.example.webclientdemo.client.RecommendationClient;
import com.example.webclientdemo.client.UserProfileClient;
import com.example.webclientdemo.dto.McpHandshake;
import com.example.webclientdemo.dto.RecommendationRequest;
import com.example.webclientdemo.model.Recommendation;
import com.example.webclientdemo.model.StreamMessage;
import com.example.webclientdemo.model.User;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 外调服务门面，展示如何统一 WebClient 调用能力。
 */
@Service
public class ExternalServiceFacade {

    private final UserProfileClient userProfileClient;
    private final RecommendationClient recommendationClient;
    private final ContentStreamClient contentStreamClient;
    private final NotificationClient notificationClient;
    private final McpClient mcpClient;

    public ExternalServiceFacade(UserProfileClient userProfileClient,
                                 RecommendationClient recommendationClient,
                                 ContentStreamClient contentStreamClient,
                                 NotificationClient notificationClient,
                                 McpClient mcpClient) {
        this.userProfileClient = userProfileClient;
        this.recommendationClient = recommendationClient;
        this.contentStreamClient = contentStreamClient;
        this.notificationClient = notificationClient;
        this.mcpClient = mcpClient;
    }

    /**
     * 同步获取用户档案。
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    public User loadUserSync(String userId) {
        return userProfileClient.getProfileSync(userId);
    }

    /**
     * 异步聚合推荐。
     *
     * @param request 推荐请求
     * @return Mono<Recommendation>
     */
    public Mono<Recommendation> loadRecommendationAsync(RecommendationRequest request) {
        return recommendationClient.fetchRecommendation(request);
    }

    /**
     * 批量获取推荐。
     *
     * @param requests 请求列表
     * @return Mono<List<Recommendation>>
     */
    public Mono<List<Recommendation>> loadBatchRecommendations(List<RecommendationRequest> requests) {
        return recommendationClient.fetchBatch(requests);
    }

    /**
     * 消费内容流。
     *
     * @return Flux<StreamMessage>
     */
    public Flux<StreamMessage> consumeContent() {
        return contentStreamClient.consumeContentFlux();
    }

    /**
     * 订阅通知 SSE。
     *
     * @return Flux<NotificationEvent>
     */
    public Flux<com.example.webclientdemo.dto.NotificationEvent> subscribeNotifications() {
        return notificationClient.subscribeNotifications();
    }

    /**
     * 连接 MCP SSE。
     *
     * @param handshake 握手
     * @return Flux<McpMessage>
     */
    public Flux<com.example.webclientdemo.dto.McpMessage> connectMcpSse(McpHandshake handshake) {
        return mcpClient.connectSse(handshake);
    }

    /**
     * 连接 MCP Streamable HTTP。
     *
     * @param handshake 握手
     * @return Flux<McpMessage>
     */
    public Flux<com.example.webclientdemo.dto.McpMessage> connectMcpStream(McpHandshake handshake) {
        return mcpClient.connectStream(handshake);
    }
}
