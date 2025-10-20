package com.example.webclient.controller;

import com.example.webclient.mcp.McpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

/**
 * MCP Mock Controller
 * 
 * <p>用于测试 WebClient MCP 调用，模拟 Model Context Protocol 服务
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/mcp")
public class McpMockController {

    private static final Logger log = LoggerFactory.getLogger(McpMockController.class);

    /**
     * MCP SSE 接口
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<McpMessage>> mcpSse() {
        log.info("MCP SSE connection established");

        return Flux.interval(Duration.ofMillis(100))
                .take(3)
                .map(i -> {
                    McpMessage message = new McpMessage();
                    message.setId("msg-" + i);
                    message.setMethod("notification");
                    message.setParams(Map.of(
                            "index", i,
                            "message", "MCP notification " + i
                    ));
                    return message;
                })
                .map(message -> ServerSentEvent.<McpMessage>builder()
                        .id(message.getId())
                        .event("mcp-message")
                        .data(message)
                        .build())
                .doOnComplete(() -> log.info("MCP SSE stream completed"));
    }

    /**
     * MCP HTTP Stream 接口
     */
    @PostMapping(value = "/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<McpMessage> mcpStream(@RequestBody McpMessage request) {
        log.info("MCP stream request received: {}", request);

        String requestId = request.getId();
        String method = request.getMethod();

        return Flux.interval(Duration.ofMillis(50))
                .take(5)
                .map(i -> {
                    McpMessage response = new McpMessage();
                    response.setId(requestId);
                    
                    if (i < 4) {
                        // 进度消息
                        response.setMethod(method + ".progress");
                        response.setResult(Map.of(
                                "progress", (i + 1) * 20,
                                "message", "Processing step " + (i + 1)
                        ));
                    } else {
                        // 最终结果
                        response.setMethod(method + ".result");
                        response.setResult(Map.of(
                                "status", "completed",
                                "data", "Final result for request " + requestId
                        ));
                    }
                    
                    return response;
                })
                .doOnComplete(() -> log.info("MCP stream completed for request: {}", requestId));
    }

    /**
     * MCP 请求接口（返回单一结果）
     */
    @PostMapping(value = "/request", produces = MediaType.APPLICATION_JSON_VALUE)
    public McpMessage mcpRequest(@RequestBody McpMessage request) {
        log.info("MCP request received: {}", request);

        McpMessage response = new McpMessage();
        response.setId(request.getId());
        response.setResult(Map.of(
                "status", "success",
                "data", "Result for method: " + request.getMethod()
        ));

        return response;
    }
}
