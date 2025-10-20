package com.example.webclient.controller;

import com.example.webclient.entity.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 流式内容 Mock Controller
 * 
 * <p>用于测试 WebClient 流式调用，模拟流式数据推送
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/stream")
public class StreamMockController {

    private static final Logger log = LoggerFactory.getLogger(StreamMockController.class);

    /**
     * 流式内容接口
     * 
     * <p>快速推送 5 条消息（测试优化：每 100ms 一条）
     */
    @GetMapping(value = "/content", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<StreamMessage> streamContent(@RequestParam(defaultValue = "default") String topic) {
        log.info("Starting stream for topic: {}", topic);

        AtomicInteger sequence = new AtomicInteger(0);

        return Flux.interval(Duration.ofMillis(100))
                .take(5)
                .map(i -> {
                    StreamMessage message = new StreamMessage();
                    message.setId("msg-" + i);
                    message.setType(topic);
                    message.setContent("Stream content " + i + " for topic: " + topic);
                    message.setSequence(sequence.incrementAndGet());
                    message.setTimestamp(LocalDateTime.now());
                    message.setFinished(i == 4);
                    return message;
                })
                .doOnNext(msg -> log.debug("Streaming message: {}", msg.getId()))
                .doOnComplete(() -> log.info("Stream completed for topic: {}", topic));
    }

    /**
     * 快速流式内容（测试用）
     * 
     * <p>快速推送 10 条消息（每 50ms 一条）
     */
    @GetMapping(value = "/fast", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<StreamMessage> streamFast() {
        log.info("Starting fast stream");

        return Flux.range(1, 10)
                .map(i -> {
                    StreamMessage message = new StreamMessage();
                    message.setId("fast-msg-" + i);
                    message.setType("fast");
                    message.setContent("Fast stream content " + i);
                    message.setSequence(i);
                    message.setTimestamp(LocalDateTime.now());
                    message.setFinished(i == 10);
                    return message;
                })
                .delayElements(Duration.ofMillis(50));
    }
}
