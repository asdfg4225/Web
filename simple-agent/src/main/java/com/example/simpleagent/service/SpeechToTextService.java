package com.example.simpleagent.service;

import com.example.simpleagent.config.SiliconFlowConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;

@Service
public class SpeechToTextService {

    private static final String SPEECH_API_URL = "https://api.siliconflow.cn/v1/audio/transcriptions";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SiliconFlowConfig config;

    @Autowired
    public SpeechToTextService(RestTemplate restTemplate, ObjectMapper objectMapper, SiliconFlowConfig config) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.config = config;
    }

    /**
     * 将音频文件转换为文字
     */
    public String transcribeAudio(File audioFile) throws Exception {
        if (!audioFile.exists()) {
            throw new IllegalArgumentException("音频文件不存在: " + audioFile.getPath());
        }

        // 检查文件大小（限制为50MB）
        if (audioFile.length() > 50 * 1024 * 1024) {
            throw new IllegalArgumentException("音频文件过大，请上传小于50MB的文件");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + config.getApiKey());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(audioFile));
        body.add("model", "FunAudioLLM/SenseVoiceSmall");
        body.add("response_format", "json");
        body.add("language", "zh");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                SPEECH_API_URL,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return parseTranscriptionResponse(response.getBody());
        } else {
            throw new RuntimeException("语音识别失败: " + response.getStatusCode() + " - " + response.getBody());
        }
    }

    /**
     * 解析语音识别响应
     */
    private String parseTranscriptionResponse(String responseBody) throws Exception {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("text").asText();

            // 格式化响应
            return formatTranscriptionResult(text);
        } catch (Exception e) {
            // 尝试其他可能的响应格式
            if (responseBody.contains("\"text\":")) {
                int start = responseBody.indexOf("\"text\":\"") + 8;
                int end = responseBody.indexOf("\"", start);
                String text = responseBody.substring(start, end);
                return formatTranscriptionResult(text);
            }
            throw new Exception("解析语音识别响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 格式化识别结果
     */
    private String formatTranscriptionResult(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "🎤 **语音识别结果**\n\n未识别到有效语音内容";
        }

        StringBuilder result = new StringBuilder();
        result.append("🎤 **语音识别结果**\n\n");

        // 检查是否为医疗相关内容
        String lowerText = text.toLowerCase();
        boolean isMedical = lowerText.contains("不舒服") || lowerText.contains("头疼") ||
                lowerText.contains("头晕") || lowerText.contains("血压") ||
                lowerText.contains("血糖") || lowerText.contains("心脏") ||
                lowerText.contains("医院") || lowerText.contains("医生");

        if (isMedical) {
            result.append("🏥 **检测到医疗相关内容**\n\n");
        }

        result.append("🗣️ **用户描述**:\n");
        result.append(text);
        result.append("\n\n---\n");
        result.append("📌 **识别完成** - 可点击「发送」将此内容发送给AI助手");

        return result.toString();
    }
}