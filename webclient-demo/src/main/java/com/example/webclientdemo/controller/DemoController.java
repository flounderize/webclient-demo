package com.example.webclientdemo.controller;

import com.example.webclientdemo.dto.McpHandshake;
import com.example.webclientdemo.dto.NotificationEvent;
import com.example.webclientdemo.dto.RecommendationRequest;
import com.example.webclientdemo.model.Recommendation;
import com.example.webclientdemo.model.StreamMessage;
import com.example.webclientdemo.model.User;
import com.example.webclientdemo.service.ExternalServiceFacade;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Demo 控制器，暴露统一外调能力供参考。
 */
@RestController
@RequestMapping("/demo")
@Validated
public class DemoController {

    private final ExternalServiceFacade facade;

    public DemoController(ExternalServiceFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/users/{userId}")
    public User getUser(@PathVariable String userId) {
        return facade.loadUserSync(userId);
    }

    @PostMapping("/recommendations")
    public Mono<Recommendation> recommend(@Valid @RequestBody RecommendationRequest request) {
        return facade.loadRecommendationAsync(request);
    }

    @PostMapping("/recommendations/batch")
    public Mono<List<Recommendation>> batchRecommend(@Valid @RequestBody List<RecommendationRequest> requests) {
        return facade.loadBatchRecommendations(requests);
    }

    @GetMapping(value = "/content/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<StreamMessage> contentStream() {
        return facade.consumeContent();
    }

    @GetMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<NotificationEvent> notifications() {
        return facade.subscribeNotifications();
    }

    @PostMapping(value = "/mcp/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<com.example.webclientdemo.dto.McpMessage> mcpSse(@RequestBody McpHandshake handshake) {
        return facade.connectMcpSse(handshake);
    }

    @PostMapping(value = "/mcp/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<com.example.webclientdemo.dto.McpMessage> mcpStream(@RequestBody McpHandshake handshake) {
        return facade.connectMcpStream(handshake);
    }
}
