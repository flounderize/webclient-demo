package com.example.webclient.client;

import com.example.webclient.dto.ApiResponse;
import com.example.webclient.dto.RecommendationRequest;
import com.example.webclient.entity.Recommendation;
import com.example.webclient.exception.WebClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * 推荐服务客户端（异步调用示例）
 * 
 * <p>演示如何使用 WebClient 进行异步调用：
 * <ul>
 *   <li>返回 Mono/Flux 供上层组合</li>
 *   <li>并发请求聚合</li>
 *   <li>响应式错误处理</li>
 *   <li>背压控制</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@Component
public class RecommendationClient {

    private static final Logger log = LoggerFactory.getLogger(RecommendationClient.class);

    private final WebClient webClient;

    public RecommendationClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8080").build();
    }

    /**
     * 异步获取推荐列表
     * 
     * <p>返回 Mono，上层可以继续使用 flatMap、zipWith 等操作符进行组合
     * 
     * @param request 推荐请求
     * @return Mono 包装的推荐列表
     */
    public Mono<List<Recommendation>> fetchAsync(RecommendationRequest request) {
        log.info("Fetching recommendations asynchronously for request: {}", request);

        return webClient.post()
                .uri("/api/recommendations")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Error response: {}", errorBody);
                                    return Mono.error(new WebClientException(
                                            "Failed to fetch recommendations: " + errorBody));
                                })
                )
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<Recommendation>>>() {})
                .timeout(Duration.ofSeconds(10))
                .map(response -> {
                    if (response == null || response.getData() == null) {
                        log.warn("Empty response for request: {}", request);
                        return List.<Recommendation>of();
                    }
                    log.info("Successfully fetched {} recommendations", response.getData().size());
                    return response.getData();
                })
                .doOnError(e -> log.error("Error fetching recommendations", e))
                // 错误时返回空列表
                .onErrorReturn(List.of());
    }

    /**
     * 异步获取单个推荐
     * 
     * @param recommendationId 推荐ID
     * @return Mono 包装的推荐对象
     */
    public Mono<Recommendation> getByIdAsync(Long recommendationId) {
        log.info("Fetching recommendation asynchronously for id: {}", recommendationId);

        return webClient.get()
                .uri("/api/recommendations/{id}", recommendationId)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(
                                        new WebClientException("Failed to get recommendation: " + errorBody)))
                )
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<Recommendation>>() {})
                .timeout(Duration.ofSeconds(5))
                .mapNotNull(ApiResponse::getData)
                .doOnSuccess(rec -> log.info("Successfully fetched recommendation: {}", recommendationId))
                .doOnError(e -> log.error("Error fetching recommendation: {}", recommendationId, e));
    }

    /**
     * 批量异步获取推荐（并发请求示例）
     * 
     * <p>使用 Flux.flatMap 并发请求多个推荐，控制并发数
     * 
     * @param ids 推荐ID列表
     * @return Flux 包装的推荐流
     */
    public Flux<Recommendation> getBatchAsync(List<Long> ids) {
        log.info("Fetching {} recommendations in batch", ids.size());

        return Flux.fromIterable(ids)
                // 并发请求，最多同时 5 个请求
                .flatMap(id -> getByIdAsync(id)
                        // 单个请求失败不影响其他请求
                        .onErrorResume(e -> {
                            log.warn("Failed to fetch recommendation {}: {}", id, e.getMessage());
                            return Mono.empty();
                        }), 5)
                .doOnComplete(() -> log.info("Batch fetch completed"));
    }

    /**
     * 组合多个异步请求（zip 示例）
     * 
     * <p>同时请求用户推荐和热门推荐，等待两个请求都完成后合并结果
     * 
     * @param userId 用户ID
     * @return Mono 包装的合并后的推荐列表
     */
    public Mono<List<Recommendation>> getCombinedRecommendations(Long userId) {
        log.info("Fetching combined recommendations for user: {}", userId);

        // 用户个性化推荐
        Mono<List<Recommendation>> personalRecommendations = fetchAsync(
                new RecommendationRequest(userId, "personal", 10));

        // 热门推荐
        Mono<List<Recommendation>> popularRecommendations = fetchAsync(
                new RecommendationRequest(null, "popular", 10));

        // 使用 zip 组合两个请求，等待都完成
        return Mono.zip(personalRecommendations, popularRecommendations)
                .map(tuple -> {
                    List<Recommendation> combined = new java.util.ArrayList<>(tuple.getT1());
                    combined.addAll(tuple.getT2());
                    log.info("Combined {} recommendations", combined.size());
                    return combined;
                })
                .timeout(Duration.ofSeconds(15))
                .doOnError(e -> log.error("Error fetching combined recommendations", e))
                .onErrorReturn(List.of());
    }

    /**
     * 串行组合请求（flatMap 示例）
     * 
     * <p>先获取推荐列表，然后对第一个推荐获取详情
     * 
     * @param request 推荐请求
     * @return Mono 包装的推荐详情
     */
    public Mono<Recommendation> getFirstRecommendationDetail(RecommendationRequest request) {
        log.info("Fetching first recommendation detail for request: {}", request);

        return fetchAsync(request)
                // 串行操作：先获取列表，再获取详情
                .flatMap(recommendations -> {
                    if (recommendations.isEmpty()) {
                        return Mono.empty();
                    }
                    Long firstId = recommendations.get(0).getId();
                    return getByIdAsync(firstId);
                })
                .doOnSuccess(rec -> {
                    if (rec != null) {
                        log.info("Successfully fetched first recommendation detail");
                    } else {
                        log.info("No recommendations found");
                    }
                });
    }
}
