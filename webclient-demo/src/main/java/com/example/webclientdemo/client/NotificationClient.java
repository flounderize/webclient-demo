package com.example.webclientdemo.client;

import com.example.webclientdemo.config.WebClientConfiguration.WebClientRegistry;
import com.example.webclientdemo.dto.NotificationEvent;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * SSE 通知客户端，处理事件流及重连。
 */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);
    private final WebClient client;

    public NotificationClient(WebClientRegistry registry) {
        this.client = registry.get("notification-service");
    }

    /**
     * 订阅通知事件并实现自动重连。
     *
     * @return Flux<NotificationEvent>
     */
    public Flux<NotificationEvent> subscribeNotifications() {
        return client.get()
            .uri("/api/notifications/sse")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<NotificationEvent>>() {})
            .map(ServerSentEvent::data)
            .retryWhen(reactor.util.retry.Retry.fixedDelay(3, Duration.ofSeconds(2))
                .doBeforeRetry(signal -> log.warn("SSE 重连，第 {} 次", signal.totalRetriesInARow() + 1)))
            .filter(event -> event != null);
    }
}
