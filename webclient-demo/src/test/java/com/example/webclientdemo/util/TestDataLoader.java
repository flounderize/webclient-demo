package com.example.webclientdemo.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 测试数据读取工具，将资源文件内容加载为字符串。
 */
public final class TestDataLoader {

    private TestDataLoader() {
    }

    /**
     * 从 classpath 读取文件。
     *
     * @param path 资源路径
     * @return 文件内容
     */
    public static String read(String path) {
        try (InputStream inputStream = TestDataLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalStateException("未找到测试资源: " + path);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("读取测试资源失败: " + path, ex);
        }
    }
}
