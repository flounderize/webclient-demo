package com.example.webclient.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.UUID;

/**
 * 追踪过滤器
 * 
 * <p>实现分布式追踪功能：
 * <ul>
 *   <li>生成或传递 traceId</li>
 *   <li>生成 spanId</li>
 *   <li>将追踪信息添加到请求头</li>
 *   <li>在 MDC 中设置追踪信息</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@Component
public class TracingExchangeFilterFunction implements ExchangeFilterFunction {

    private static final Logger log = LoggerFactory.getLogger(TracingExchangeFilterFunction.class);

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";
    private static final String TRACE_ID_KEY = "traceId";
    private static final String SPAN_ID_KEY = "spanId";

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return Mono.deferContextual(contextView -> {
            // 从 Reactor Context 或 MDC 中获取 traceId，如果不存在则生成新的
            String traceId = getOrGenerateTraceId(contextView);
            String spanId = generateSpanId();

            // 构建新的请求，添加追踪头
            ClientRequest tracedRequest = ClientRequest.from(request)
                    .header(TRACE_ID_HEADER, traceId)
                    .header(SPAN_ID_HEADER, spanId)
                    .build();

            log.debug("Adding trace headers - traceId: {}, spanId: {}", traceId, spanId);

            // 在 MDC 中设置追踪信息（用于日志）
            MDC.put(TRACE_ID_KEY, traceId);
            MDC.put(SPAN_ID_KEY, spanId);

            try {
                return next.exchange(tracedRequest)
                        .contextWrite(Context.of(TRACE_ID_KEY, traceId, SPAN_ID_KEY, spanId));
            } finally {
                // 清理 MDC（在响应式编程中，这里的 finally 可能不够完善，但作为示例已足够）
                MDC.remove(TRACE_ID_KEY);
                MDC.remove(SPAN_ID_KEY);
            }
        });
    }

    /**
     * 获取或生成 traceId
     */
    private String getOrGenerateTraceId(ContextView contextView) {
        // 尝试从 Reactor Context 获取
        if (contextView.hasKey(TRACE_ID_KEY)) {
            return contextView.get(TRACE_ID_KEY);
        }

        // 尝试从 MDC 获取
        String mdcTraceId = MDC.get(TRACE_ID_KEY);
        if (mdcTraceId != null && !mdcTraceId.isEmpty()) {
            return mdcTraceId;
        }

        // 生成新的 traceId
        return generateTraceId();
    }

    /**
     * 生成新的 traceId
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成新的 spanId
     */
    private String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
