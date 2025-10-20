package com.example.webclient.springaimcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring WebFlux SSE 接收器示例
 * 
 * <p>演示如何使用 Spring WebFlux 接收和处理 SSE 事件。
 * 这与纯 WebClient SSE 消费不同，这里是服务端接收 SSE 数据的场景。
 * 
 * <p>使用场景：
 * <ul>
 *   <li>服务端接收来自其他服务的 SSE 推送</li>
 *   <li>SSE 数据聚合和转发</li>
 *   <li>SSE 事件处理和持久化</li>
 *   <li>SSE 数据转换和过滤</li>
 * </ul>
 * 
 * <p>与 WebClient SSE 的区别：
 * <ul>
 *   <li>WebClient SSE：客户端订阅远程 SSE 端点</li>
 *   <li>WebFlux SSE 接收器：服务端暴露端点接收 SSE 数据</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/springai/sse")
public class SseReceiverController {

    private static final Logger log = LoggerFactory.getLogger(SseReceiverController.class);

    private final SseDataProcessor sseDataProcessor;

    public SseReceiverController(SseDataProcessor sseDataProcessor) {
        this.sseDataProcessor = sseDataProcessor;
    }

    /**
     * 接收 SSE 数据流并处理
     * 
     * <p>客户端通过 POST 发送 SSE 数据流到此端点
     * 
     * @param events SSE 事件流
     * @return 处理结果
     */
    @PostMapping(value = "/receive", consumes = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<Map<String, Object>> receiveSseStream(@RequestBody Flux<ServerSentEvent<String>> events) {
        log.info("Receiving SSE stream");

        return events
            .doOnNext(event -> {
                log.info("Received SSE event - type: {}, id: {}, data: {}", 
                    event.event(), event.id(), event.data());
                
                // 处理事件
                sseDataProcessor.processEvent(event);
            })
            .count()
            .map(count -> Map.of(
                "status", "success",
                "eventsReceived", count,
                "message", "SSE stream processed successfully"
            ));
    }

    /**
     * 接收并转发 SSE 流
     * 
     * <p>接收 SSE 流，处理后转发给订阅者
     * 
     * @param events SSE 事件流
     * @return 处理后的 SSE 流
     */
    @PostMapping(value = "/forward", 
        consumes = MediaType.TEXT_EVENT_STREAM_VALUE,
        produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> receiveAndForwardSseStream(
            @RequestBody Flux<ServerSentEvent<String>> events) {
        
        log.info("Receiving and forwarding SSE stream");

        return events
            .doOnNext(event -> log.debug("Forwarding event: {}", event.event()))
            .map(event -> {
                // 可以在这里对事件进行转换或增强
                String data = event.data();
                String transformedData = sseDataProcessor.transformEventData(data);
                
                return ServerSentEvent.<String>builder()
                    .event(event.event())
                    .id(event.id())
                    .data(transformedData)
                    .build();
            })
            .doOnComplete(() -> log.info("SSE stream forwarding completed"));
    }

    /**
     * 聚合多个 SSE 源
     * 
     * <p>从多个 SSE 源接收数据并聚合
     * 
     * @param sourceId 数据源ID
     * @param events SSE 事件流
     * @return 确认响应
     */
    @PostMapping(value = "/aggregate/{sourceId}", consumes = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<Map<String, Object>> aggregateSseSource(
            @PathVariable String sourceId,
            @RequestBody Flux<ServerSentEvent<String>> events) {
        
        log.info("Aggregating SSE source: {}", sourceId);

        return sseDataProcessor.aggregateSource(sourceId, events)
            .map(count -> Map.of(
                "status", "success",
                "sourceId", sourceId,
                "eventsAggregated", count
            ));
    }

    /**
     * 获取聚合的 SSE 数据
     * 
     * @return 聚合后的 SSE 流
     */
    @GetMapping(value = "/aggregated", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, Object>>> getAggregatedStream() {
        log.info("Client subscribing to aggregated stream");
        return sseDataProcessor.getAggregatedStream();
    }
}

/**
 * SSE 数据处理服务
 */
@Service
class SseDataProcessor {

    private static final Logger log = LoggerFactory.getLogger(SseDataProcessor.class);

    private final ObjectMapper objectMapper;
    private final Map<String, Sinks.Many<ServerSentEvent<String>>> sourceSinks = new ConcurrentHashMap<>();
    private final Sinks.Many<Map<String, Object>> aggregatedSink = 
        Sinks.many().multicast().onBackpressureBuffer();

    public SseDataProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 处理单个 SSE 事件
     */
    public void processEvent(ServerSentEvent<String> event) {
        String eventType = event.event();
        String data = event.data();
        
        log.debug("Processing event - type: {}, data: {}", eventType, data);
        
        try {
            // 解析事件数据
            if (data != null && !data.isEmpty()) {
                Map<String, Object> eventData = objectMapper.readValue(data, Map.class);
                
                // 添加元数据
                eventData.put("receivedAt", System.currentTimeMillis());
                eventData.put("eventType", eventType);
                
                // 发送到聚合流
                aggregatedSink.tryEmitNext(eventData);
                
                // 执行业务逻辑
                handleBusinessLogic(eventType, eventData);
            }
        } catch (Exception e) {
            log.error("Error processing event", e);
        }
    }

    /**
     * 转换事件数据
     */
    public String transformEventData(String data) {
        try {
            if (data == null || data.isEmpty()) {
                return data;
            }
            
            // 解析并转换
            Map<String, Object> parsed = objectMapper.readValue(data, Map.class);
            parsed.put("transformed", true);
            parsed.put("transformedAt", System.currentTimeMillis());
            
            return objectMapper.writeValueAsString(parsed);
        } catch (Exception e) {
            log.error("Error transforming data", e);
            return data;
        }
    }

    /**
     * 聚合数据源
     */
    public Mono<Long> aggregateSource(String sourceId, Flux<ServerSentEvent<String>> events) {
        log.info("Starting aggregation for source: {}", sourceId);
        
        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().multicast().onBackpressureBuffer();
        sourceSinks.put(sourceId, sink);
        
        return events
            .doOnNext(event -> {
                sink.tryEmitNext(event);
                processEvent(event);
            })
            .doOnComplete(() -> {
                log.info("Source {} stream completed", sourceId);
                sourceSinks.remove(sourceId);
            })
            .count();
    }

    /**
     * 获取聚合流
     */
    public Flux<ServerSentEvent<Map<String, Object>>> getAggregatedStream() {
        return aggregatedSink.asFlux()
            .map(data -> ServerSentEvent.<Map<String, Object>>builder()
                .event("aggregated")
                .data(data)
                .build())
            .mergeWith(
                // 添加心跳
                Flux.interval(Duration.ofSeconds(30))
                    .map(seq -> ServerSentEvent.<Map<String, Object>>builder()
                        .event("heartbeat")
                        .data(Map.of("timestamp", System.currentTimeMillis()))
                        .build())
            );
    }

    /**
     * 处理业务逻辑
     */
    private void handleBusinessLogic(String eventType, Map<String, Object> eventData) {
        // 根据事件类型执行不同的业务逻辑
        switch (eventType) {
            case "message":
                log.info("Processing message event: {}", eventData);
                // 消息处理逻辑
                break;
            case "notification":
                log.info("Processing notification event: {}", eventData);
                // 通知处理逻辑
                break;
            case "alert":
                log.warn("Processing alert event: {}", eventData);
                // 告警处理逻辑
                break;
            default:
                log.debug("Processing generic event: {}", eventType);
        }
    }

    /**
     * 获取特定源的流
     */
    public Flux<ServerSentEvent<String>> getSourceStream(String sourceId) {
        Sinks.Many<ServerSentEvent<String>> sink = sourceSinks.get(sourceId);
        if (sink == null) {
            return Flux.empty();
        }
        return sink.asFlux();
    }

    /**
     * 获取所有活动源的ID
     */
    public java.util.Set<String> getActiveSources() {
        return sourceSinks.keySet();
    }
}
