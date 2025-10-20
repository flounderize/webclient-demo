package com.example.webclient.controller;

import com.example.webclient.dto.ApiResponse;
import com.example.webclient.dto.RecommendationRequest;
import com.example.webclient.entity.Recommendation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 推荐 Mock Controller
 * 
 * <p>用于测试 WebClient 异步调用，模拟推荐服务 API
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationMockController {

    private static final Logger log = LoggerFactory.getLogger(RecommendationMockController.class);

    private final Map<Long, Recommendation> recommendationStore = new ConcurrentHashMap<>();

    public RecommendationMockController() {
        initTestData();
    }

    private void initTestData() {
        for (int i = 1; i <= 20; i++) {
            Recommendation rec = new Recommendation();
            rec.setId((long) i);
            rec.setUserId((long) ((i % 5) + 1));
            rec.setType(i % 2 == 0 ? "personal" : "popular");
            rec.setTitle("Recommendation " + i);
            rec.setDescription("Description for recommendation " + i);
            rec.setScore(Math.random() * 10);
            rec.setTags(List.of("tag" + (i % 3), "tag" + (i % 5)));
            rec.setCreatedAt(LocalDateTime.now());
            recommendationStore.put(rec.getId(), rec);
        }
    }

    /**
     * 获取推荐列表
     */
    @PostMapping
    public ApiResponse<List<Recommendation>> getRecommendations(@RequestBody RecommendationRequest request) {
        log.info("Getting recommendations: {}", request);

        List<Recommendation> results = new ArrayList<>(recommendationStore.values());

        // 根据用户ID过滤
        if (request.getUserId() != null) {
            results = results.stream()
                    .filter(r -> request.getUserId().equals(r.getUserId()))
                    .collect(Collectors.toList());
        }

        // 根据类型过滤
        if (request.getCategory() != null) {
            results = results.stream()
                    .filter(r -> request.getCategory().equals(r.getType()))
                    .collect(Collectors.toList());
        }

        // 根据最小分数过滤
        if (request.getMinScore() != null) {
            results = results.stream()
                    .filter(r -> r.getScore() >= request.getMinScore())
                    .collect(Collectors.toList());
        }

        // 限制数量
        if (request.getLimit() != null && request.getLimit() > 0) {
            results = results.stream()
                    .limit(request.getLimit())
                    .collect(Collectors.toList());
        }

        return ApiResponse.success(results);
    }

    /**
     * 获取单个推荐
     */
    @GetMapping("/{id}")
    public ApiResponse<Recommendation> getRecommendation(@PathVariable Long id) {
        log.info("Getting recommendation: {}", id);
        Recommendation recommendation = recommendationStore.get(id);
        if (recommendation == null) {
            return ApiResponse.error(404, "Recommendation not found");
        }
        return ApiResponse.success(recommendation);
    }
}
