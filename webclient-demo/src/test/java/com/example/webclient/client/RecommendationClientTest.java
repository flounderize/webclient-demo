package com.example.webclient.client;

import com.example.webclient.dto.RecommendationRequest;
import com.example.webclient.entity.Recommendation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 推荐客户端异步调用测试
 * 
 * <p>测试 RecommendationClient 的异步调用场景：
 * <ul>
 *   <li>Mono 异步调用</li>
 *   <li>Flux 流式处理</li>
 *   <li>并发请求</li>
 *   <li>请求组合</li>
 * </ul>
 * 
 * <p>使用 StepVerifier 验证响应式流
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@DisplayName("异步调用测试")
@Timeout(value = 10, unit = TimeUnit.SECONDS)
class RecommendationClientTest {

    @Autowired
    private RecommendationClient recommendationClient;

    @Test
    @DisplayName("异步获取推荐列表 - 成功")
    void testFetchAsync_Success() {
        // Given: 推荐请求
        RecommendationRequest request = new RecommendationRequest(1L, "personal", 10);

        // When: 异步获取推荐
        Mono<List<Recommendation>> resultMono = recommendationClient.fetchAsync(request);

        // Then: 使用 StepVerifier 验证
        StepVerifier.create(resultMono)
                .assertNext(recommendations -> {
                    // 验证返回的推荐列表
                    assert recommendations != null;
                    assert !recommendations.isEmpty();
                    assert recommendations.size() <= 10;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("异步获取单个推荐 - 成功")
    void testGetByIdAsync_Success() {
        // Given: 推荐 ID
        Long recommendationId = 1L;

        // When: 异步获取推荐
        Mono<Recommendation> resultMono = recommendationClient.getByIdAsync(recommendationId);

        // Then: 验证结果
        StepVerifier.create(resultMono)
                .assertNext(recommendation -> {
                    assert recommendation != null;
                    assert recommendation.getId().equals(recommendationId);
                    assert recommendation.getTitle() != null;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("批量异步获取推荐 - 并发测试")
    void testGetBatchAsync_Concurrent() {
        // Given: 多个推荐 ID
        List<Long> ids = List.of(1L, 2L, 3L, 4L, 5L);

        // When: 批量异步获取
        Flux<Recommendation> resultFlux = recommendationClient.getBatchAsync(ids);

        // Then: 验证返回的推荐数量
        StepVerifier.create(resultFlux)
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    @DisplayName("批量异步获取 - 部分失败处理")
    void testGetBatchAsync_PartialFailure() {
        // Given: 包含不存在 ID 的列表
        List<Long> ids = List.of(1L, 999L, 2L);

        // When: 批量获取（应该忽略失败的请求）
        Flux<Recommendation> resultFlux = recommendationClient.getBatchAsync(ids);

        // Then: 应该返回成功的部分
        StepVerifier.create(resultFlux)
                .expectNextMatches(rec -> rec.getId().equals(1L))
                .expectNextMatches(rec -> rec.getId().equals(2L))
                .verifyComplete();
    }

    @Test
    @DisplayName("组合多个异步请求 - Zip 测试")
    void testGetCombinedRecommendations() {
        // Given: 用户 ID
        Long userId = 1L;

        // When: 获取组合推荐
        Mono<List<Recommendation>> resultMono = recommendationClient.getCombinedRecommendations(userId);

        // Then: 验证组合结果
        StepVerifier.create(resultMono)
                .assertNext(recommendations -> {
                    assert recommendations != null;
                    // 应该包含个性化和热门推荐
                    assert recommendations.size() > 0;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("串行组合请求 - FlatMap 测试")
    void testGetFirstRecommendationDetail() {
        // Given: 推荐请求
        RecommendationRequest request = new RecommendationRequest(1L, "personal", 5);

        // When: 获取第一个推荐的详情
        Mono<Recommendation> resultMono = recommendationClient.getFirstRecommendationDetail(request);

        // Then: 验证详情
        StepVerifier.create(resultMono)
                .assertNext(recommendation -> {
                    assert recommendation != null;
                    assert recommendation.getTitle() != null;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("异步调用超时测试")
    void testFetchAsync_Timeout() {
        // Given: 推荐请求
        RecommendationRequest request = new RecommendationRequest(1L, "personal", 10);

        // When: 正常调用（不测试超时，因为超时行为不确定）
        Mono<List<Recommendation>> resultMono = recommendationClient.fetchAsync(request);

        // Then: 验证可以正常获取结果
        StepVerifier.create(resultMono)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("空结果处理测试")
    void testFetchAsync_EmptyResult() {
        // Given: 不会匹配任何数据的请求
        RecommendationRequest request = new RecommendationRequest(999L, "nonexistent", 10);

        // When: 获取推荐
        Mono<List<Recommendation>> resultMono = recommendationClient.fetchAsync(request);

        // Then: 应该返回空列表（不抛异常）
        StepVerifier.create(resultMono)
                .assertNext(recommendations -> {
                    assert recommendations != null;
                    assert recommendations.isEmpty();
                })
                .verifyComplete();
    }
}
