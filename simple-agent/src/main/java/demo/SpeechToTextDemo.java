package demo;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.io.File;

public class SpeechToTextDemo {

    private static final String API_URL = "https://api.siliconflow.cn/v1/audio/transcriptions";
    private static final String API_KEY = "sk-iftbyuggkgiaxkpltndicbtrxpibfmegavlzdaxvioanivda";

    private final RestTemplate restTemplate;

    public SpeechToTextDemo() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 将音频文件转换为文字
     * @param audioFile 音频文件
     * @return 识别出的文字
     */
    public String transcribeAudio(File audioFile) throws Exception {
        if (!audioFile.exists()) {
            throw new IllegalArgumentException("音频文件不存在: " + audioFile.getPath());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // 添加文件
        body.add("file", new FileSystemResource(audioFile));

        // 添加参数
        body.add("model", "FunAudioLLM/SenseVoiceSmall");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                API_URL,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            // 解析返回的JSON
            String responseBody = response.getBody();
            // 这里假设返回格式为 {"text": "识别的文字内容"}
            return extractTextFromResponse(responseBody);
        } else {
            throw new RuntimeException("语音识别失败: " + response.getStatusCode() + " - " + response.getBody());
        }
    }

    private String extractTextFromResponse(String jsonResponse) {
        try {
            // 简单解析JSON，实际应该使用Jackson
            if (jsonResponse.contains("\"text\":")) {
                int start = jsonResponse.indexOf("\"text\":\"") + 8;
                int end = jsonResponse.indexOf("\"", start);
                return jsonResponse.substring(start, end);
            }
            return jsonResponse;
        } catch (Exception e) {
            return jsonResponse;
        }
    }

    // 支持多种音频格式
    public boolean isSupportedAudioFormat(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".mp3") ||
                lowerName.endsWith(".wav") ||
                lowerName.endsWith(".m4a") ||
                lowerName.endsWith(".flac");
    }

    // 使用方法
    public static void main(String[] args) {
        try {
            SpeechToTextDemo demo = new SpeechToTextDemo();

            // 指定音频文件路径
            String audioPath = "C:\\Users\\15PRO\\Desktop\\experiment\\1\\实训\\simple-agent-2 - 副本\\edge_output.wav";
            File audioFile = new File(audioPath);

            if (!audioFile.exists()) {
                System.out.println("请先在项目目录下放置测试音频文件: " + audioPath);
                return;
            }

            if (!demo.isSupportedAudioFormat(audioPath)) {
                System.out.println("不支持的音频格式");
                return;
            }

            System.out.println("=== 开始语音识别 ===");
            System.out.println("音频文件: " + audioFile.getName());
            System.out.println("文件大小: " + audioFile.length() + " 字节");

            String transcribedText = demo.transcribeAudio(audioFile);

            System.out.println("\n识别结果:");
            System.out.println(transcribedText);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}