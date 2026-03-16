package com.example.simpleagent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SiliconFlowConfig {
    @Value("${siliconflow.api.key}")
    private String apiKey;

    @Value("${siliconflow.api.url}")
    private String apiUrl;

    public String getApiKey() { return apiKey; }
    public String getApiUrl() { return apiUrl; }
}