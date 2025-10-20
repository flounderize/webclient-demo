package com.example.webclientdemo.client;

import com.example.webclientdemo.AbstractMockServerTest;
import com.example.webclientdemo.dto.RecommendationRequest;
import com.example.webclientdemo.model.Recommendation;
import com.example.webclientdemo.util.TestDataLoader;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 异步客户端测试，验证 Mono/Flux 调用链。
 */
class AsyncClientTests extends AbstractMockServerTest {

    @Autowired
    private RecommendationClient client;

    @Test
    @DisplayName("Mono 推荐请求")
    void shouldFetchRecommendationAsMono() {
        enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(TestDataLoader.read("data/recommendation.json")));

        RecommendationRequest request = new RecommendationRequest();
        request.setUserId("u-001");
        request.setContext(Map.of("lang", "zh-CN"));

        Mono<Recommendation> mono = client.fetchRecommendation(request);

        StepVerifier.create(mono)
            .expectNextMatches(rec -> rec.getId().equals("rec-1") && rec.getContent().contains("Hello"))
            .verifyComplete();
    }

    @Test
    @DisplayName("Flux 聚合多个推荐")
    void shouldFetchBatchRecommendations() {
        enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(TestDataLoader.read("data/recommendation.json")));
        enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(TestDataLoader.read("data/recommendation.json")));

        RecommendationRequest request = new RecommendationRequest();
        request.setUserId("u-002");
        request.setContext(Map.of("scene", "search"));

        Mono<List<Recommendation>> result = client.fetchBatch(List.of(request, request));

        List<Recommendation> recommendations = result.block();
        assertThat(recommendations).hasSize(2);
    }
}
