package com.example.webclient.client;

import com.example.webclient.entity.StreamMessage;
import com.example.webclient.exception.WebClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 流式内容客户端（响应式流调用示例）
 * 
 * <p>演示如何使用 WebClient 处理流式响应：
 * <ul>
 *   <li>接收 Flux 流式数据</li>
 *   <li>处理 NDJSON (Newline Delimited JSON)</li>
 *   <li>背压控制</li>
 *   <li>流式错误处理</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@Component
public class ContentStreamClient {

    private static final Logger log = LoggerFactory.getLogger(ContentStreamClient.class);

    private final WebClient webClient;

    public ContentStreamClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8080").build();
    }

    /**
     * 消费流式内容
     * 
     * <p>接收服务端的流式响应，返回 Flux 供上层订阅消费
     * <p>适用场景：处理大量数据流、实时数据推送等
     * 
     * @param topic 主题
     * @return Flux 包装的消息流
     */
    public Flux<StreamMessage> consumeContentFlux(String topic) {
        log.info("Starting to consume content stream for topic: {}", topic);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/stream/content")
                        .queryParam("topic", topic)
                        .build())
                // 接受 NDJSON 格式
                .accept(MediaType.APPLICATION_NDJSON)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Stream error: {}", errorBody);
                                    return Mono.error(new WebClientException(
                                            "Failed to consume stream: " + errorBody));
                                })
                )
                .bodyToFlux(StreamMessage.class)
                // 设置超时（流式调用可以设置较长的超时时间）
                .timeout(Duration.ofMinutes(5))
                // 每条消息记录日志
                .doOnNext(message -> log.debug("Received stream message: {}", message))
                // 流开始时记录
                .doOnSubscribe(subscription -> log.info("Subscribed to content stream"))
                // 流完成时记录
                .doOnComplete(() -> log.info("Content stream completed"))
                // 流取消时记录
                .doOnCancel(() -> log.info("Content stream cancelled"))
                // 流错误时记录
                .doOnError(e -> log.error("Error in content stream", e))
                // 错误时继续（可选，根据业务需求决定）
                .onErrorResume(e -> {
                    log.warn("Resuming stream after error: {}", e.getMessage());
                    return Flux.empty();
                });
    }

    /**
     * 消费流式内容（带背压控制）
     * 
     * <p>使用 limitRate 控制背压，防止消费者处理速度过慢导致内存溢出
     * 
     * @param topic 主题
     * @param prefetch 预取数量
     * @return Flux 包装的消息流
     */
    public Flux<StreamMessage> consumeWithBackpressure(String topic, int prefetch) {
        log.info("Consuming stream with backpressure, topic: {}, prefetch: {}", topic, prefetch);

        return consumeContentFlux(topic)
                // 限制请求速率，每次最多请求 prefetch 条
                .limitRate(prefetch)
                .doOnRequest(n -> log.debug("Requested {} items", n));
    }

    /**
     * 流式数据过滤和转换
     * 
     * <p>演示流式数据的过滤、映射等操作
     * 
     * @param topic 主题
     * @param keyword 过滤关键词
     * @return Flux 包装的过滤后的消息流
     */
    public Flux<String> consumeAndFilter(String topic, String keyword) {
        log.info("Consuming and filtering stream, topic: {}, keyword: {}", topic, keyword);

        return consumeContentFlux(topic)
                // 过滤：只保留包含关键词的消息
                .filter(message -> message.getContent() != null && 
                        message.getContent().contains(keyword))
                // 转换：提取内容
                .map(StreamMessage::getContent)
                // 去重（根据内容）
                .distinct()
                .doOnNext(content -> log.debug("Filtered content: {}", content));
    }

    /**
     * 批量处理流式数据
     * 
     * <p>将流式数据分组批量处理，提高效率
     * 
     * @param topic 主题
     * @param batchSize 批量大小
     * @return Flux 包装的消息批次
     */
    public Flux<java.util.List<StreamMessage>> consumeInBatches(String topic, int batchSize) {
        log.info("Consuming stream in batches, topic: {}, batchSize: {}", topic, batchSize);

        return consumeContentFlux(topic)
                // 缓冲：每 batchSize 条消息打包一次，或者超时后打包
                .bufferTimeout(batchSize, Duration.ofSeconds(5))
                .doOnNext(batch -> log.info("Received batch of {} messages", batch.size()));
    }

    /**
     * 窗口化处理流式数据
     * 
     * <p>按时间窗口处理流式数据
     * 
     * @param topic 主题
     * @param windowDuration 窗口时长
     * @return Flux 包装的窗口消息流
     */
    public Flux<Flux<StreamMessage>> consumeInTimeWindows(String topic, Duration windowDuration) {
        log.info("Consuming stream in time windows, topic: {}, window: {}", topic, windowDuration);

        return consumeContentFlux(topic)
                // 按时间窗口分组
                .window(windowDuration)
                .doOnNext(window -> log.info("New time window started"));
    }
}
