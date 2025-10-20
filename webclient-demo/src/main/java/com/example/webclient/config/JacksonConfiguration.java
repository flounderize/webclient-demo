package com.example.webclient.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.ZoneOffset;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 自定义配置，统一时间格式与空值策略。
 */
@Configuration
public class JacksonConfiguration {

    /**
     * 调整 ObjectMapper 行为。
     *
     * @return Jackson2ObjectMapperBuilderCustomizer
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> builder
            .timeZone(java.util.TimeZone.getTimeZone(ZoneOffset.UTC))
            .simpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .serializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
    }
}
