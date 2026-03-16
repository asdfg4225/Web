package com.example.simpleagent.service;

import com.example.simpleagent.config.SiliconFlowConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.*;

@Service
public class OCRService {

    private static final String OCR_API_URL = "https://api.siliconflow.cn/v1/chat/completions";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SiliconFlowConfig config;

    @Autowired
    public OCRService(RestTemplate restTemplate, ObjectMapper objectMapper, SiliconFlowConfig config) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.config = config;
    }

    /**
     * 识别图片中的文字
     */
    public String recognizeText(File imageFile) throws Exception {
        if (!imageFile.exists()) {
            throw new IllegalArgumentException("图片文件不存在: " + imageFile.getPath());
        }

        // 将图片转换为Base64
        String base64Image = fileToBase64(imageFile);

        // 构建请求
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + config.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-ai/DeepSeek-OCR");
        requestBody.put("stream", false);
        requestBody.put("max_tokens", 4096);
        requestBody.put("temperature", 0.1);

        // 构建消息
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");

        List<Map<String, Object>> contentList = new ArrayList<>();

        // 文本指令
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", "请识别这张图片中的所有文字，保持原文的格式和顺序。如果是药品说明书，请提取药品名称、用法用量、注意事项等信息。");
        contentList.add(textContent);

        // 图片内容
        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image_url");

        Map<String, String> imageUrl = new HashMap<>();
        imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
        imageContent.put("image_url", imageUrl);
        contentList.add(imageContent);

        userMessage.put("content", contentList);
        messages.add(userMessage);
        requestBody.put("messages", messages);

        // 发送请求
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(OCR_API_URL, request, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return parseOCRResponse(response.getBody());
        } else {
            throw new RuntimeException("OCR识别失败: " + response.getStatusCode() + " - " + response.getBody());
        }
    }

    /**
     * 将文件转换为Base64
     */
    private String fileToBase64(File file) throws Exception {
        byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
        return Base64.getEncoder().encodeToString(fileContent);
    }

    /**
     * 解析OCR响应
     */
    private String parseOCRResponse(String responseBody) throws Exception {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");

            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                String content = message.path("content").asText();

                // 格式化响应
                return formatOCRResult(content);
            }

            return "未找到识别结果";
        } catch (Exception e) {
            throw new Exception("解析OCR响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 格式化OCR结果
     */
    private String formatOCRResult(String content) {
        StringBuilder result = new StringBuilder();
        result.append("🖼️ **OCR识别结果**\n\n");

        // 提取关键信息
        if (content.contains("药品") || content.contains("药") || content.contains("片") || content.contains("胶囊")) {
            result.append("💊 **检测到药品信息**\n\n");
        }

        result.append(content);
        result.append("\n\n---\n");
        result.append("📌 **识别完成** - 请核对以上信息");

        return result.toString();
    }
}