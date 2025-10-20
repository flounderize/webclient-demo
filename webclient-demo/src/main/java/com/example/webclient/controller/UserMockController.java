package com.example.webclient.controller;

import com.example.webclient.dto.ApiResponse;
import com.example.webclient.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户 Mock Controller
 * 
 * <p>用于测试 WebClient 同步调用，模拟真实的 REST API
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/users")
public class UserMockController {

    private static final Logger log = LoggerFactory.getLogger(UserMockController.class);

    private final Map<Long, User> userStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public UserMockController() {
        // 初始化一些测试数据
        initTestData();
    }

    private void initTestData() {
        for (int i = 1; i <= 5; i++) {
            User user = new User();
            user.setId((long) i);
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setPhone("1234567890" + i);
            user.setAge(20 + i);
            user.setAddress("Address " + i);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userStore.put(user.getId(), user);
        }
        idGenerator.set(6);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/{id}")
    public ApiResponse<User> getUser(@PathVariable Long id) {
        log.info("Getting user: {}", id);
        User user = userStore.get(id);
        if (user == null) {
            return ApiResponse.error(404, "User not found");
        }
        return ApiResponse.success(user);
    }

    /**
     * 创建用户
     */
    @PostMapping
    public ApiResponse<User> createUser(@RequestBody User user) {
        log.info("Creating user: {}", user.getUsername());
        user.setId(idGenerator.getAndIncrement());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userStore.put(user.getId(), user);
        return ApiResponse.success(user);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public ApiResponse<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        log.info("Updating user: {}", id);
        User existingUser = userStore.get(id);
        if (existingUser == null) {
            return ApiResponse.error(404, "User not found");
        }
        user.setId(id);
        user.setUpdatedAt(LocalDateTime.now());
        user.setCreatedAt(existingUser.getCreatedAt());
        userStore.put(id, user);
        return ApiResponse.success(user);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        log.info("Deleting user: {}", id);
        User user = userStore.remove(id);
        if (user == null) {
            return ApiResponse.error(404, "User not found");
        }
        return ApiResponse.success(null);
    }
}
