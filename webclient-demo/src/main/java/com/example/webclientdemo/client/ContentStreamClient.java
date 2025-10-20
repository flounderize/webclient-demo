package com.example.webclientdemo.client;

import com.example.webclientdemo.config.WebClientConfiguration.WebClientRegistry;
import com.example.webclientdemo.model.StreamMessage;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * 内容流客户端，处理 NDJSON 或 chunked 传输。
 */
@Component
public class ContentStreamClient {

    private final WebClient client;

    public ContentStreamClient(WebClientRegistry registry) {
        this.client = registry.get("content-service");
    }

    /**
     * 订阅内容流，转为 Flux<StreamMessage>。
     *
     * @return Flux<StreamMessage>
     */
    public Flux<StreamMessage> consumeContentFlux() {
        return client.get()
            .uri("/api/content/stream")
            .accept(MediaType.APPLICATION_NDJSON)
            .retrieve()
            .bodyToFlux(StreamMessage.class)
            .onBackpressureBuffer(128);
    }
}
