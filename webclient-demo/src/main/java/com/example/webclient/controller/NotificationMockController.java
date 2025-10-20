package com.example.webclient.controller;

import com.example.webclient.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 通知 Mock Controller (SSE)
 * 
 * <p>用于测试 WebClient SSE 调用，模拟 Server-Sent Events 推送
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationMockController {

    private static final Logger log = LoggerFactory.getLogger(NotificationMockController.class);

    /**
     * 订阅通知（SSE）
     * 
     * <p>快速推送 3 条通知（测试优化：每 100ms 一条）
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> subscribe(@RequestParam String userId) {
        log.info("User {} subscribed to notifications", userId);

        // 通知流：快速推送 3 条，每 100ms 一条
        Flux<ServerSentEvent<Object>> notifications = Flux.interval(Duration.ofMillis(100))
                .take(3)
                .map(i -> {
                    Notification notification = new Notification();
                    notification.setId("notif-" + userId + "-" + i);
                    notification.setUserId(userId);
                    notification.setType(i % 2 == 0 ? "info" : "alert");
                    notification.setTitle("Notification " + i);
                    notification.setMessage("Message " + i + " for user " + userId);
                    notification.setPriority(i % 3 == 0 ? "high" : "normal");
                    notification.setTimestamp(LocalDateTime.now());
                    notification.setRead(false);
                    return notification;
                })
                .map(notification -> ServerSentEvent.builder()
                        .id(notification.getId())
                        .event("notification")
                        .data(notification)
                        .build());

        // 心跳流：只发送一次（用于测试）
        Flux<ServerSentEvent<Object>> heartbeats = Flux.just(
                ServerSentEvent.builder()
                        .event("heartbeat")
                        .data("ping")
                        .build()
        ).delayElements(Duration.ofMillis(50));

        // 合并通知和心跳
        return Flux.merge(notifications, heartbeats)
                .doOnSubscribe(sub -> log.info("SSE stream started for user: {}", userId))
                .doOnComplete(() -> log.info("SSE stream completed for user: {}", userId))
                .doOnCancel(() -> log.info("SSE stream cancelled for user: {}", userId));
    }

    /**
     * 简单的 SSE 测试接口
     */
    @GetMapping(value = "/simple", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> simple() {
        log.info("Simple SSE stream started");

        return Flux.interval(Duration.ofMillis(50))
                .take(3)
                .map(i -> ServerSentEvent.<String>builder()
                        .id(String.valueOf(i))
                        .event("message")
                        .data("Event " + i)
                        .build());
    }
}
