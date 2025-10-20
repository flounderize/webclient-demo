package com.example.webclient.client;

import com.example.webclient.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * 通知客户端（SSE 调用示例）
 * 
 * <p>演示如何使用 WebClient 接收 Server-Sent Events：
 * <ul>
 *   <li>订阅 SSE 事件流</li>
 *   <li>处理不同类型的事件</li>
 *   <li>自动重连机制</li>
 *   <li>心跳检测</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final WebClient sseWebClient;

    public NotificationClient(WebClient sseWebClient) {
        this.sseWebClient = sseWebClient;
    }

    /**
     * 订阅通知（SSE）
     * 
     * <p>建立 SSE 连接，持续接收服务端推送的通知
     * <p>适用场景：实时通知、消息推送、状态更新等
     * 
     * @param userId 用户ID
     * @return Flux 包装的通知流
     */
    public Flux<Notification> subscribeNotifications(String userId) {
        log.info("Subscribing to notifications for user: {}", userId);

        return sseWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/notifications/subscribe")
                        .queryParam("userId", userId)
                        .build())
                .retrieve()
                .bodyToFlux(ServerSentEvent.class)
                // 提取事件数据
                .mapNotNull(event -> {
                    String eventType = event.event();
                    Object data = event.data();
                    String id = event.id();
                    
                    log.debug("Received SSE event - type: {}, id: {}, data: {}", eventType, id, data);
                    
                    // 根据事件类型处理
                    if ("notification".equals(eventType) && data != null) {
                        try {
                            // 假设数据已经是 Notification 对象或 Map
                            if (data instanceof Notification) {
                                return (Notification) data;
                            }
                            // 这里可以根据实际情况进行转换
                            return null;
                        } catch (Exception e) {
                            log.error("Failed to parse notification data", e);
                            return null;
                        }
                    } else if ("heartbeat".equals(eventType)) {
                        log.trace("Received heartbeat");
                        return null;
                    }
                    return null;
                })
                // 过滤掉 null 值
                .filter(notification -> notification != null)
                .doOnSubscribe(subscription -> log.info("SSE subscription started"))
                .doOnNext(notification -> log.info("Received notification: {}", notification.getId()))
                .doOnComplete(() -> log.info("SSE stream completed"))
                .doOnCancel(() -> log.info("SSE subscription cancelled"))
                .doOnError(e -> log.error("Error in SSE stream", e))
                // 自动重连：发生错误时等待 5 秒后重试，最多重试 10 次
                .retryWhen(Retry.fixedDelay(10, Duration.ofSeconds(5))
                        .doBeforeRetry(retrySignal -> 
                            log.info("Retrying SSE connection, attempt: {}", retrySignal.totalRetries() + 1)));
    }

    /**
     * 订阅特定类型的通知
     * 
     * @param userId 用户ID
     * @param notificationType 通知类型
     * @return Flux 包装的通知流
     */
    public Flux<Notification> subscribeByType(String userId, String notificationType) {
        log.info("Subscribing to {} notifications for user: {}", notificationType, userId);

        return subscribeNotifications(userId)
                // 只保留指定类型的通知
                .filter(notification -> notificationType.equals(notification.getType()))
                .doOnNext(notification -> 
                    log.info("Received {} notification: {}", notificationType, notification.getId()));
    }

    /**
     * 订阅通知并限制数量
     * 
     * <p>只接收前 N 条通知后自动取消订阅
     * 
     * @param userId 用户ID
     * @param count 接收数量
     * @return Flux 包装的通知流
     */
    public Flux<Notification> subscribeWithLimit(String userId, int count) {
        log.info("Subscribing to {} notifications for user: {}", count, userId);

        return subscribeNotifications(userId)
                // 只接收前 count 条
                .take(count)
                .doOnComplete(() -> log.info("Received {} notifications, unsubscribing", count));
    }

    /**
     * 订阅通知并设置超时
     * 
     * <p>如果在指定时间内没有收到通知，则超时结束
     * 
     * @param userId 用户ID
     * @param timeout 超时时间
     * @return Flux 包装的通知流
     */
    public Flux<Notification> subscribeWithTimeout(String userId, Duration timeout) {
        log.info("Subscribing to notifications with timeout: {}", timeout);

        return subscribeNotifications(userId)
                .timeout(timeout)
                .doOnError(e -> log.warn("Notification subscription timeout"))
                .onErrorResume(e -> Flux.empty());
    }

    /**
     * 订阅原始 SSE 事件
     * 
     * <p>返回原始的 ServerSentEvent 对象，包含完整的事件信息
     * 
     * @param userId 用户ID
     * @return Flux 包装的 SSE 事件流
     */
    public Flux<ServerSentEvent<String>> subscribeRawEvents(String userId) {
        log.info("Subscribing to raw SSE events for user: {}", userId);

        return sseWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/notifications/subscribe")
                        .queryParam("userId", userId)
                        .build())
                .retrieve()
                .bodyToFlux(new org.springframework.core.ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .doOnNext(event -> log.debug("Raw SSE event - type: {}, id: {}, data: {}", 
                        event.event(), event.id(), event.data()))
                .retryWhen(Retry.fixedDelay(10, Duration.ofSeconds(5)));
    }

    /**
     * 订阅并收集通知到列表
     * 
     * <p>收集所有通知到一个列表中返回（仅用于测试或短时间订阅）
     * 
     * @param userId 用户ID
     * @param duration 收集时长
     * @return Mono 包装的通知列表
     */
    public Mono<java.util.List<Notification>> collectNotifications(String userId, Duration duration) {
        log.info("Collecting notifications for {} seconds", duration.getSeconds());

        return subscribeNotifications(userId)
                .take(duration)
                .collectList()
                .doOnSuccess(list -> log.info("Collected {} notifications", list.size()));
    }
}
