package com.example.webclientdemo.client;

import com.example.webclientdemo.AbstractMockServerTest;
import com.example.webclientdemo.dto.McpHandshake;
import com.example.webclientdemo.dto.McpMessage;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * MCP SSE 调用测试。
 */
class McpSseClientTests extends AbstractMockServerTest {

    @Autowired
    private McpClient client;

    @Test
    @DisplayName("解析 MCP SSE 消息")
    void shouldReceiveMcpSseMessages() {
        String body = "id:1\n" +
            "event:mcp-message\n" +
            "data:{\"id\":\"msg-1\",\"type\":\"delta\",\"content\":{\"text\":\"hello\"}}\n\n" +
            "id:2\n" +
            "event:mcp-message\n" +
            "data:{\"id\":\"msg-2\",\"type\":\"delta\",\"content\":{\"text\":\"world\"}}\n\n";

        enqueue(new MockResponse()
            .setHeader("Content-Type", "text/event-stream")
            .setBody(body));

        McpHandshake handshake = new McpHandshake();
        handshake.setVersion("1.0");
        handshake.setCapabilities(java.util.Map.of("stream", true));

        Flux<McpMessage> flux = client.connectSse(handshake).take(2);

        StepVerifier.create(flux)
            .expectNextMatches(msg -> "msg-1".equals(msg.getId()))
            .expectNextMatches(msg -> "msg-2".equals(msg.getId()))
            .verifyComplete();
    }
}
