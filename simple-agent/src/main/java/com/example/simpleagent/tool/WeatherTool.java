package com.example.simpleagent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class WeatherTool implements Tool {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public WeatherTool(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_weather";
    }

    @Override
    public String getDescription() {
        return "获取指定城市天气（中文），参数：{\"city\": \"城市名，如'杭州'\"}";
    }

    @Override
    public String execute(Map<String, Object> args) {
        String city = (String) args.get("city");
        if (city == null || city.trim().isEmpty()) {
            return "❌ 城市名称不能为空";
        }

        try {
            // 使用 wttr.in API
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8.toString());
            String url = "https://wttr.in/" + encodedCity + "?format=j1&lang=zh";

            System.out.println("🌤️ 天气查询URL: " + url);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Accept", "application/json");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            System.out.println("🌤️ 天气响应状态: " + response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                System.out.println("🌤️ 天气响应内容: " + responseBody);

                JsonNode data = objectMapper.readTree(responseBody);
                JsonNode currentCondition = data.path("current_condition");

                if (currentCondition.isArray() && currentCondition.size() > 0) {
                    JsonNode condition = currentCondition.get(0);
                    String temp = condition.path("temp_C").asText("未知");
                    String humidity = condition.path("humidity").asText("未知");
                    String windSpeed = condition.path("windspeedKmph").asText("未知");

                    // 获取天气描述
                    String desc = "未知";
                    JsonNode weatherDesc = condition.path("weatherDesc");
                    if (weatherDesc.isArray() && weatherDesc.size() > 0) {
                        desc = weatherDesc.get(0).path("value").asText("未知");
                    }

                    return String.format("📍 %s天气：\n" +
                                    "🌡️ 温度：%s°C\n" +
                                    "🌧️ 天气：%s\n" +
                                    "💧 湿度：%s%%\n" +
                                    "💨 风速：%s km/h",
                            city, temp, desc, humidity, windSpeed);
                } else {
                    return "❌ 无法解析天气数据，请稍后再试";
                }
            } else {
                return "❌ 天气查询失败，HTTP状态码：" + response.getStatusCode();
            }
        } catch (Exception e) {
            System.out.println("🌤️ 天气查询异常: " + e.getMessage());
            e.printStackTrace();

            // 备选方案：使用国内天气API
            try {
                return getWeatherBackup(city);
            } catch (Exception ex) {
                return "❌ 天气查询服务暂时不可用，请稍后再试。";
            }
        }
    }

    private String getWeatherBackup(String city) throws Exception {
        // 备选天气API - 使用和风天气的公开接口（需要注册API key）
        String url = String.format("https://devapi.qweather.com/v7/weather/now?location=%s&key=你的API密钥",
                URLEncoder.encode(city, StandardCharsets.UTF_8.toString()));

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "simpleagent/1.0");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode data = objectMapper.readTree(response.getBody());
            if (data.path("code").asText().equals("200")) {
                JsonNode now = data.path("now");
                String temp = now.path("temp").asText();
                String text = now.path("text").asText();
                return String.format("📍 %s当前天气：%s，温度：%s°C", city, text, temp);
            }
        }

        return "❌ 天气查询失败，请检查城市名称是否正确";
    }
}