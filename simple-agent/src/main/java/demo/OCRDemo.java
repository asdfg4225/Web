package demo;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class OCRDemo {

    private static final String API_URL = "https://api.siliconflow.cn/v1/chat/completions";
    private static final String API_KEY = "sk-iftbyuggkgiaxkpltndicbtrxpibfmegavlzdaxvioanivda";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OCRDemo() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * OCR识别图片中的文字
     */
    public String recognizeText(File imageFile) throws Exception {
        if (!imageFile.exists()) {
            throw new IllegalArgumentException("图片文件不存在: " + imageFile.getPath());
        }

        // 1. 将图片转换为Base64
        String base64Image = encodeFileToBase64(imageFile);

        // 2. 构建请求
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-ai/DeepSeek-OCR");  // 使用DeepSeek-OCR模型
        requestBody.put("stream", false);
        requestBody.put("max_tokens", 4096);

        // 3. 构建消息
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");

        List<Map<String, Object>> contentList = new ArrayList<>();

        // 文本指令
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", "请识别这张图片中的所有文字，保持原文的格式和顺序。如果是表格，请用表格形式表示。");
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

        // 4. 发送请求
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        System.out.println("🔄 发送OCR识别请求...");
        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            String result = parseOCRResponse(response.getBody());
            System.out.println("✅ OCR识别成功");
            return result;
        } else {
            throw new RuntimeException("OCR识别失败: " + response.getStatusCode());
        }
    }

    /**
     * 批量识别多张图片
     */
    public Map<String, String> batchRecognize(List<File> imageFiles) throws Exception {
        Map<String, String> results = new LinkedHashMap<>();

        for (File imageFile : imageFiles) {
            System.out.println("处理图片: " + imageFile.getName());
            try {
                String text = recognizeText(imageFile);
                results.put(imageFile.getName(), text);
            } catch (Exception e) {
                results.put(imageFile.getName(), "识别失败: " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * 将文件编码为Base64
     */
    private String encodeFileToBase64(File file) throws Exception {
        byte[] fileContent = Files.readAllBytes(file.toPath());
        return Base64.getEncoder().encodeToString(fileContent);
    }

    /**
     * 解析OCR响应
     */
    private String parseOCRResponse(String responseBody) throws Exception {
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");

            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                return (String) message.get("content");
            }

            // 备用解析方式
            return responseBody;

        } catch (Exception e) {
            System.err.println("解析响应失败: " + e.getMessage());
            return responseBody;
        }
    }

    /**
     * 支持的图片格式检查
     */
    public boolean isSupportedImageFormat(String fileName) {
        if (fileName == null) return false;
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".jpg") ||
                lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".png") ||
                lowerName.endsWith(".bmp") ||
                lowerName.endsWith(".gif") ||
                lowerName.endsWith(".webp");
    }

    /**
     * 保存识别结果到文件
     */
    public void saveResultToFile(String text, String outputPath) throws Exception {
        try (java.io.FileWriter writer = new java.io.FileWriter(outputPath)) {
            writer.write("=== OCR识别结果 ===\n\n");
            writer.write("识别时间: " + new java.util.Date() + "\n\n");
            writer.write(text);
            System.out.println("✅ 结果已保存到: " + outputPath);
        }
    }

    // 使用方法
    public static void main(String[] args) {
        try {
            OCRDemo demo = new OCRDemo();

            System.out.println("=== DeepSeek-OCR 文字识别测试 ===\n");

            // 指定测试图片
            String imagePath = "C:\\Users\\15PRO\\Pictures\\Screenshots\\屏幕截图 2025-11-13 223756.png";  // 替换为您的图片路径
            File imageFile = new File(imagePath);

            if (!imageFile.exists()) {
                System.out.println("⚠️ 测试图片不存在: " + imagePath);
                System.out.println("请在项目目录下放置测试图片，支持的格式：jpg, png, bmp, gif");
                return;
            }

            if (!demo.isSupportedImageFormat(imagePath)) {
                System.out.println("❌ 不支持的图片格式: " + imagePath);
                return;
            }

            System.out.println("📷 图片信息:");
            System.out.println("  文件名: " + imageFile.getName());
            System.out.println("  文件大小: " + imageFile.length() + " 字节");
            System.out.println("  最后修改: " + new java.util.Date(imageFile.lastModified()));

            System.out.println("\n🔄 开始OCR识别...");
            long startTime = System.currentTimeMillis();

            String ocrResult = demo.recognizeText(imageFile);

            long endTime = System.currentTimeMillis();
            System.out.println("✅ 识别完成，耗时: " + (endTime - startTime) + "ms\n");

            System.out.println("📄 识别结果:");
            System.out.println(ocrResult);

            // 保存结果
            String outputPath = "ocr_result_" + System.currentTimeMillis() + ".txt";
            demo.saveResultToFile(ocrResult, outputPath);

        } catch (Exception e) {
            System.err.println("❌ OCR识别失败:");
            e.printStackTrace();
        }
    }
}