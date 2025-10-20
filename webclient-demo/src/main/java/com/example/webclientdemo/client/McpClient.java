package com.example.webclientdemo.client;

import com.example.webclientdemo.config.WebClientConfiguration.WebClientRegistry;
import com.example.webclientdemo.dto.McpHandshake;
import com.example.webclientdemo.dto.McpMessage;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * MCP 客户端，兼容 SSE 与 Streamable HTTP 模式。
 */
@Component
public class McpClient {

    private static final Logger log = LoggerFactory.getLogger(McpClient.class);
    private final WebClient sseClient;
    private final WebClient streamClient;
    private final AtomicLong sequence = new AtomicLong(0);

    public McpClient(WebClientRegistry registry) {
        this.sseClient = registry.get("mcp-sse-service");
        this.streamClient = registry.get("mcp-stream-service");
    }

    /**
     * 执行 MCP 握手并以 SSE 获取消息。
     *
     * @param handshake 握手参数
     * @return Flux<McpMessage>
     */
    public Flux<McpMessage> connectSse(McpHandshake handshake) {
        return sseClient.post()
            .uri("/mcp/sse/connect")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(handshake)
            .retrieve()
            .bodyToFlux(new org.springframework.core.ParameterizedTypeReference<ServerSentEvent<McpMessage>>() {})
            .map(ServerSentEvent::data)
            .doOnNext(msg -> log.debug("收到 MCP SSE 消息 {}", msg != null ? msg.getId() : "null"))
            .filter(msg -> msg != null);
    }

    /**
     * 通过 Streamable HTTP 与 MCP 服务交互。
     *
     * @param handshake 握手
     * @return Flux<McpMessage>
     */
    public Flux<McpMessage> connectStream(McpHandshake handshake) {
        return streamClient.post()
            .uri("/mcp/stream/connect")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromPublisher(Mono.just(handshake), McpHandshake.class))
            .retrieve()
            .bodyToFlux(McpMessage.class)
            .map(this::assignSequence)
            .doOnSubscribe(subscription -> sequence.set(0));
    }

    private McpMessage assignSequence(McpMessage message) {
        if (message.getId() == null) {
            message.setId("seq-" + sequence.incrementAndGet());
        }
        return message;
    }
}
