package com.example.webclient.client;

import com.example.webclient.entity.User;
import com.example.webclient.exception.WebClientException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * 用户客户端同步调用测试
 * 
 * <p>测试 UserProfileClient 的各种同步调用场景：
 * <ul>
 *   <li>正常的 CRUD 操作</li>
 *   <li>错误处理</li>
 *   <li>超时控制</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@DisplayName("同步调用测试")
@Timeout(value = 10, unit = TimeUnit.SECONDS)
class UserProfileClientTest {

    @Autowired
    private UserProfileClient userProfileClient;

    @Test
    @DisplayName("同步获取用户信息 - 成功")
    void testGetProfileSync_Success() {
        // Given: 存在的用户 ID
        String userId = "1";

        // When: 同步获取用户信息
        User user = userProfileClient.getProfileSync(userId);

        // Then: 验证用户信息
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getUsername()).isNotBlank();
        assertThat(user.getEmail()).isNotBlank();
    }

    @Test
    @DisplayName("同步获取用户信息 - 用户不存在")
    void testGetProfileSync_NotFound() {
        // Given: 不存在的用户 ID
        String userId = "999";

        // When & Then: 应该抛出异常
        assertThatThrownBy(() -> userProfileClient.getProfileSync(userId))
                .isInstanceOf(WebClientException.class);
    }

    @Test
    @DisplayName("同步创建用户 - 成功")
    void testCreateUserSync_Success() {
        // Given: 新用户信息
        User newUser = new User();
        newUser.setUsername("testuser");
        newUser.setEmail("testuser@example.com");
        newUser.setPhone("1234567890");
        newUser.setAge(25);

        // When: 创建用户
        User createdUser = userProfileClient.createUserSync(newUser);

        // Then: 验证创建成功
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getUsername()).isEqualTo("testuser");
        assertThat(createdUser.getEmail()).isEqualTo("testuser@example.com");
    }

    @Test
    @DisplayName("同步更新用户 - 成功")
    void testUpdateUserSync_Success() {
        // Given: 存在的用户
        String userId = "1";
        User updatedUser = new User();
        updatedUser.setUsername("updateduser");
        updatedUser.setEmail("updated@example.com");

        // When: 更新用户
        User result = userProfileClient.updateUserSync(userId, updatedUser);

        // Then: 验证更新成功
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("updateduser");
    }

    @Test
    @DisplayName("同步删除用户 - 成功")
    void testDeleteUserSync_Success() {
        // Given: 先创建一个用户
        User newUser = new User();
        newUser.setUsername("tobedeleted");
        newUser.setEmail("delete@example.com");
        User createdUser = userProfileClient.createUserSync(newUser);

        // When: 删除用户
        assertThatNoException()
                .isThrownBy(() -> userProfileClient.deleteUserSync(createdUser.getId().toString()));

        // Then: 验证用户已被删除
        assertThatThrownBy(() -> userProfileClient.getProfileSync(createdUser.getId().toString()))
                .isInstanceOf(WebClientException.class);
    }

    @Test
    @DisplayName("批量操作测试")
    void testBatchOperations() {
        // Given: 创建多个用户
        for (int i = 0; i < 3; i++) {
            User user = new User();
            user.setUsername("batchuser" + i);
            user.setEmail("batch" + i + "@example.com");
            userProfileClient.createUserSync(user);
        }

        // When & Then: 获取这些用户
        for (int i = 1; i <= 3; i++) {
            User user = userProfileClient.getProfileSync(String.valueOf(i));
            assertThat(user).isNotNull();
        }
    }
}
