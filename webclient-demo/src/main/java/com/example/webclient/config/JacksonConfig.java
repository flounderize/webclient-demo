package com.example.webclient.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 配置类
 * 
 * <p>统一配置 JSON 序列化/反序列化规则：
 * <ul>
 *   <li>日期时间格式化</li>
 *   <li>空值处理策略</li>
 *   <li>未知属性处理</li>
 *   <li>枚举序列化</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 创建自定义的 ObjectMapper
     * 
     * @return 配置好的 ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        // 创建 JavaTimeModule 并配置日期时间格式
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // LocalDateTime 格式化
        javaTimeModule.addSerializer(LocalDateTime.class, 
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
        javaTimeModule.addDeserializer(LocalDateTime.class, 
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
        
        // LocalDate 格式化
        javaTimeModule.addSerializer(LocalDate.class, 
                new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        javaTimeModule.addDeserializer(LocalDate.class, 
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));

        return Jackson2ObjectMapperBuilder.json()
                .modules(javaTimeModule)
                // 序列化配置
                .serializationInclusion(JsonInclude.Include.NON_NULL) // 忽略 null 值
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // 日期不序列化为时间戳
                .featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS) // 允许序列化空对象
                // 反序列化配置
                .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // 忽略未知属性
                .featuresToEnable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT) // 空字符串转 null
                .build();
    }
}
