package com.example.webclient.client;

import com.example.webclient.entity.StreamMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 流式内容客户端测试
 * 
 * <p>测试 ContentStreamClient 的流式调用场景：
 * <ul>
 *   <li>Flux 流式数据接收</li>
 *   <li>背压控制</li>
 *   <li>流式数据过滤和转换</li>
 *   <li>批量处理</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@DisplayName("响应式流调用测试")
@Timeout(value = 10, unit = TimeUnit.SECONDS)
class ContentStreamClientTest {

    @Autowired
    private ContentStreamClient contentStreamClient;

    @Test
    @DisplayName("消费流式内容 - 成功")
    void testConsumeContentFlux_Success() {
        // Given: 主题
        String topic = "test";

        // When: 消费流式内容（只取前3条）
        Flux<StreamMessage> resultFlux = contentStreamClient.consumeContentFlux(topic)
                .take(3)
                .timeout(Duration.ofSeconds(3));

        // Then: 验证流式数据
        StepVerifier.create(resultFlux)
                .expectNextMatches(message -> message != null && message.getContent() != null)
                .expectNextCount(2) // 期待再收到 2 条消息
                .verifyComplete();
    }

    @Test
    @DisplayName("消费流式内容 - 背压控制")
    void testConsumeWithBackpressure() {
        // Given: 主题和预取数量
        String topic = "test";
        int prefetch = 2;

        // When: 消费流式内容（带背压，只取前3条）
        Flux<StreamMessage> resultFlux = contentStreamClient.consumeWithBackpressure(topic, prefetch)
                .take(3)
                .timeout(Duration.ofSeconds(3));

        // Then: 验证流式数据
        StepVerifier.create(resultFlux, prefetch)
                .expectNextCount(2)
                .thenRequest(1)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("流式数据过滤 - 关键词过滤")
    void testConsumeAndFilter() {
        // Given: 主题和关键词
        String topic = "test";
        String keyword = "content";

        // When: 消费并过滤（只取前3条）
        Flux<String> resultFlux = contentStreamClient.consumeAndFilter(topic, keyword)
                .take(3)
                .timeout(Duration.ofSeconds(3));

        // Then: 验证所有结果都包含关键词
        StepVerifier.create(resultFlux)
                .expectNextMatches(content -> content.contains(keyword))
                .expectNextCount(2) // 再收到 2 条
                .verifyComplete();
    }

    @Test
    @DisplayName("批量处理流式数据")
    void testConsumeInBatches() {
        // Given: 主题和批量大小
        String topic = "test";
        int batchSize = 2;

        // When: 批量消费（只取2个批次）
        Flux<List<StreamMessage>> resultFlux = contentStreamClient.consumeInBatches(topic, batchSize)
                .take(2)
                .timeout(Duration.ofSeconds(3));

        // Then: 验证批次
        StepVerifier.create(resultFlux)
                .assertNext(batch -> {
                    assert batch != null;
                    assert !batch.isEmpty(); // 第一个批次应该有数据
                    assert batch.size() <= batchSize;
                })
                .assertNext(batch -> {
                    assert batch != null;
                    assert !batch.isEmpty(); // 第二个批次应该有数据
                    assert batch.size() <= batchSize;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("窗口化处理流式数据")
    void testConsumeInTimeWindows() {
        // Given: 主题和窗口时长
        String topic = "test";
        Duration windowDuration = Duration.ofMillis(200); // 200ms 窗口

        // When: 窗口化消费，取前3个窗口并统计每个窗口的消息数
        Flux<Long> resultFlux = contentStreamClient.consumeInTimeWindows(topic, windowDuration)
                .flatMap(window -> window.count()) // 统计每个窗口的消息数量
                .take(3) // 只取前3个窗口的统计结果
                .timeout(Duration.ofSeconds(3));

        // Then: 验证至少收到一些窗口的统计信息
        StepVerifier.create(resultFlux)
                .expectNextCount(3) // 应该有3个窗口
                .verifyComplete();
    }

    @Test
    @DisplayName("流式数据超时测试")
    void testConsumeContentFlux_Timeout() {
        // Given: 主题
        String topic = "test";

        // When: 设置短超时，只取第一条
        Flux<StreamMessage> resultFlux = contentStreamClient.consumeContentFlux(topic)
                .take(1)
                .timeout(Duration.ofSeconds(1));

        // Then: 验证至少收到一条
        StepVerifier.create(resultFlux)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("流式数据限制数量测试")
    void testConsumeContentFlux_Take() {
        // Given: 主题
        String topic = "test";

        // When: 只取前 3 条
        Flux<StreamMessage> resultFlux = contentStreamClient.consumeContentFlux(topic).take(3);

        // Then: 验证只收到 3 条
        StepVerifier.create(resultFlux)
                .expectNextCount(3)
                .verifyComplete();
    }
}
