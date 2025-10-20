package com.example.webclientdemo.client;

import com.example.webclientdemo.AbstractMockServerTest;
import com.example.webclientdemo.dto.McpHandshake;
import com.example.webclientdemo.dto.McpMessage;
import com.example.webclientdemo.util.TestDataLoader;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * MCP Streamable HTTP 测试。
 */
class McpStreamClientTests extends AbstractMockServerTest {

    @Autowired
    private McpClient client;

    @Test
    @DisplayName("分块 JSON 流解析")
    void shouldParseMcpStreamMessages() {
        String chunks = TestDataLoader.read("data/mcp-stream.ndjson");
        enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(chunks));

        McpHandshake handshake = new McpHandshake();
        handshake.setVersion("1.0");
        handshake.setCapabilities(java.util.Map.of("delta", true));

        Flux<McpMessage> flux = client.connectStream(handshake).take(2);

        StepVerifier.create(flux)
            .expectNextMatches(msg -> msg.getId() != null && msg.getContent() != null)
            .expectNextMatches(msg -> msg.getId() != null && msg.getContent() != null)
            .verifyComplete();
    }
}
