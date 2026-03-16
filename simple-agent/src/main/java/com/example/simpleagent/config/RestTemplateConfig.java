package com.example.simpleagent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // 创建RequestFactory
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);

        // 创建RestTemplate
        RestTemplate restTemplate = new RestTemplate(factory);

        // 设置字符编码为UTF-8
        restTemplate.getMessageConverters()
                .forEach(converter -> {
                    if (converter instanceof StringHttpMessageConverter) {
                        ((StringHttpMessageConverter) converter).setDefaultCharset(StandardCharsets.UTF_8);
                    }
                });

        return restTemplate;
    }
}