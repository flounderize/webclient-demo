package com.example.webclientdemo.client;

import com.example.webclientdemo.AbstractMockServerTest;
import com.example.webclientdemo.model.User;
import com.example.webclientdemo.util.TestDataLoader;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 同步客户端测试，校验阻塞调用与重试策略。
 */
class SyncClientTests extends AbstractMockServerTest {

    @Autowired
    private UserProfileClient client;

    @Test
    @DisplayName("阻塞式获取用户信息")
    void shouldFetchUserSynchronously() throws Exception {
        enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(TestDataLoader.read("data/user.json")));

        User user = client.getProfileSync("u-001");

        assertThat(user.getId()).isEqualTo("u-001");
        assertThat(user.getName()).isEqualTo("示例用户");
    }
}
