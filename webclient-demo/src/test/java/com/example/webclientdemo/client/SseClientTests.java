package com.example.webclientdemo.client;

import com.example.webclientdemo.AbstractMockServerTest;
import com.example.webclientdemo.dto.NotificationEvent;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * SSE 客户端测试，验证事件流解析与重试机制。
 */
class SseClientTests extends AbstractMockServerTest {

    @Autowired
    private NotificationClient client;

    @Test
    @DisplayName("SSE 事件解析")
    void shouldParseSseEvents() {
        String body = "id:1\n" +
            "event:message\n" +
            "data:{\"id\":\"n1\",\"type\":\"notify\",\"payload\":\"hello\",\"occurredAt\":\"2024-01-01T08:00:00Z\"}\n\n" +
            "id:2\n" +
            "event:message\n" +
            "data:{\"id\":\"n2\",\"type\":\"notify\",\"payload\":\"world\",\"occurredAt\":\"2024-01-01T08:00:01Z\"}\n\n";

        enqueue(new MockResponse()
            .setHeader("Content-Type", "text/event-stream")
            .setBody(body));

        Flux<NotificationEvent> flux = client.subscribeNotifications().take(2);

        StepVerifier.create(flux)
            .expectNextMatches(event -> "n1".equals(event.getId()))
            .expectNextMatches(event -> "n2".equals(event.getId()))
            .verifyComplete();
    }
}
