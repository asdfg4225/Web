package com.example.simpleagent.controller;

import com.example.simpleagent.service.EdgeTTSService;
import com.example.simpleagent.service.OCRService;
import com.example.simpleagent.service.SpeechToTextService;
import com.example.simpleagent.utils.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/agent")
public class MultimediaController {

    private final ObjectMapper objectMapper;
    private final EdgeTTSService edgeTTSService;
    private final OCRService ocrService;
    private final SpeechToTextService speechToTextService;

    @Autowired
    public MultimediaController(ObjectMapper objectMapper,
                                EdgeTTSService edgeTTSService,
                                OCRService ocrService,
                                SpeechToTextService speechToTextService) {
        this.objectMapper = objectMapper;
        this.edgeTTSService = edgeTTSService;
        this.ocrService = ocrService;
        this.speechToTextService = speechToTextService;
    }

    /**
     * 语音转文字
     */
    @PostMapping("/speech-to-text")
    public ResponseEntity<?> speechToText(@RequestParam("audio_file") MultipartFile audioFile) {
        try {
            // 验证文件
            if (audioFile.isEmpty()) {
                return ResponseEntity.badRequest().body("请上传音频文件");
            }

            String originalFilename = audioFile.getOriginalFilename();

            // 验证文件类型
            if (!FileUtils.isSupportedAudio(originalFilename)) {
                return ResponseEntity.badRequest().body("不支持的文件格式，请上传 MP3、WAV、M4A、FLAC 或 OGG 格式的音频文件");
            }

            // 保存临时文件
            String tempFilePath = FileUtils.saveUploadedFile(audioFile);
            File tempFile = new File(tempFilePath);

            try {
                // 调用语音识别服务
                String recognizedText = speechToTextService.transcribeAudio(tempFile);

                // 构建响应
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("filename", originalFilename);
                response.put("filesize", audioFile.getSize());
                response.put("text", recognizedText);
                response.put("message", "语音识别完成");

                return ResponseEntity.ok(response);

            } finally {
                // 清理临时文件
                FileUtils.deleteFile(tempFilePath);
            }

        } catch (Exception e) {
            e.printStackTrace();

            // 返回错误信息
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "语音识别失败: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * OCR文字识别
     */
    @PostMapping("/ocr")
    public ResponseEntity<?> ocr(@RequestParam("image_file") MultipartFile imageFile) {
        try {
            // 验证文件
            if (imageFile.isEmpty()) {
                return ResponseEntity.badRequest().body("请上传图片文件");
            }

            String originalFilename = imageFile.getOriginalFilename();

            // 验证文件类型
            if (!FileUtils.isSupportedImage(originalFilename)) {
                return ResponseEntity.badRequest().body("不支持的文件格式，请上传 JPG、JPEG、PNG、BMP、GIF 或 WebP 格式的图片");
            }

            // 保存临时文件
            String tempFilePath = FileUtils.saveUploadedFile(imageFile);
            File tempFile = new File(tempFilePath);

            try {
                // 调用OCR服务
                String ocrResult = ocrService.recognizeText(tempFile);

                // 构建响应
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("filename", originalFilename);
                response.put("filesize", imageFile.getSize());
                response.put("text", ocrResult);
                response.put("message", "文字识别完成");

                return ResponseEntity.ok(response);

            } finally {
                // 清理临时文件
                FileUtils.deleteFile(tempFilePath);
            }

        } catch (Exception e) {
            e.printStackTrace();

            // 返回错误信息
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "文字识别失败: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 文字转语音
     */
    @PostMapping("/text-to-speech")
    public ResponseEntity<?> textToSpeech(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");

            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("请输入要转换的文本");
            }

            if (text.length() > 1000) {
                return ResponseEntity.badRequest().body("文本长度不能超过1000个字符");
            }

            // 调用TTS服务
            String audioFilename = edgeTTSService.textToSpeech(text);

            // 获取文件信息
            String filePath = "uploads/" + audioFilename;
            File audioFile = new File(filePath);

            if (!audioFile.exists()) {
                return ResponseEntity.badRequest().body("语音文件生成失败");
            }

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("text", text);
            response.put("audio_filename", audioFilename);
            response.put("audio_url", "/api/agent/audio/" + audioFilename);
            response.put("filesize", audioFile.length());
            response.put("message", "语音合成完成");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();

            // 返回错误信息
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "语音合成失败: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 获取生成的音频文件
     */
    @GetMapping("/audio/{filename}")
    public ResponseEntity<byte[]> getAudioFile(@PathVariable String filename) {
        try {
            byte[] audioData = edgeTTSService.getAudioFile(filename);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/wav"));
            headers.setContentDisposition(ContentDisposition.builder("inline")
                    .filename(filename)
                    .build());
            headers.setContentLength(audioData.length);

            return new ResponseEntity<>(audioData, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 批量文件处理（可选）
     */
    @PostMapping("/batch-process")
    public ResponseEntity<?> batchProcess(@RequestParam("files") MultipartFile[] files) {
        try {
            Map<String, Object> results = new HashMap<>();

            for (MultipartFile file : files) {
                try {
                    String originalFilename = file.getOriginalFilename();

                    // 根据文件类型处理
                    if (FileUtils.isSupportedAudio(originalFilename)) {
                        String tempFilePath = FileUtils.saveUploadedFile(file);
                        File tempFile = new File(tempFilePath);

                        try {
                            String result = speechToTextService.transcribeAudio(tempFile);
                            results.put(originalFilename, result);
                        } finally {
                            FileUtils.deleteFile(tempFilePath);
                        }

                    } else if (FileUtils.isSupportedImage(originalFilename)) {
                        String tempFilePath = FileUtils.saveUploadedFile(file);
                        File tempFile = new File(tempFilePath);

                        try {
                            String result = ocrService.recognizeText(tempFile);
                            results.put(originalFilename, result);
                        } finally {
                            FileUtils.deleteFile(tempFilePath);
                        }
                    }

                } catch (Exception e) {
                    results.put(file.getOriginalFilename(), "处理失败: " + e.getMessage());
                }
            }

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("批量处理失败: " + e.getMessage());
        }
    }

    /**
     * 清理临时文件
     */
    @PostMapping("/cleanup")
    public ResponseEntity<?> cleanup() {
        try {
            FileUtils.cleanupExpiredFiles();
            return ResponseEntity.ok("文件清理完成");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("文件清理失败: " + e.getMessage());
        }
    }
}