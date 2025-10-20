package com.example.webclientdemo.client;

import com.example.webclientdemo.AbstractMockServerTest;
import com.example.webclientdemo.model.StreamMessage;
import com.example.webclientdemo.util.TestDataLoader;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * 流式客户端测试，校验 NDJSON / chunked 数据处理。
 */
class StreamingClientTests extends AbstractMockServerTest {

    @Autowired
    private ContentStreamClient client;

    @Test
    @DisplayName("消费流式内容")
    void shouldConsumeStreamMessages() {
        enqueue(new MockResponse()
            .setHeader("Content-Type", "application/x-ndjson")
            .setBody(TestDataLoader.read("data/stream.ndjson")));

        Flux<StreamMessage> flux = client.consumeContentFlux();

        StepVerifier.create(flux.take(3))
            .expectNextCount(3)
            .verifyComplete();
    }
}
