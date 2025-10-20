package com.example.webclientdemo.client;

import com.example.webclientdemo.config.WebClientConfiguration.WebClientRegistry;
import com.example.webclientdemo.dto.RecommendationRequest;
import com.example.webclientdemo.model.Recommendation;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 推荐服务客户端，演示 Mono/Flux 异步调用。
 */
@Component
public class RecommendationClient {

    private final WebClient client;

    public RecommendationClient(WebClientRegistry registry) {
        this.client = registry.get("recommendation-service");
    }

    /**
     * 获取推荐结果（Mono），供上层拼接。
     *
     * @param request 推荐请求参数
     * @return Mono<Recommendation>
     */
    public Mono<Recommendation> fetchRecommendation(RecommendationRequest request) {
        return client.post()
            .uri("/api/recommendations")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Recommendation.class);
    }

    /**
     * 获取多个推荐结果（Flux），演示流式聚合拦截。
     *
     * @param request 推荐请求
     * @return Flux<Recommendation>
     */
    public Flux<Recommendation> fetchRecommendationFlux(RecommendationRequest request) {
        return client.post()
            .uri("/api/recommendations/stream")
            .bodyValue(request)
            .retrieve()
            .bodyToFlux(Recommendation.class);
    }

    /**
     * 并发收集多个推荐结果。
     *
     * @param requests 请求列表
     * @return Mono<List<Recommendation>>
     */
    public Mono<List<Recommendation>> fetchBatch(List<RecommendationRequest> requests) {
        return Flux.fromIterable(requests)
            .flatMap(this::fetchRecommendation)
            .collectList();
    }
}
