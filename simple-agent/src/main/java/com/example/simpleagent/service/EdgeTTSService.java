package com.example.simpleagent.service;

import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class EdgeTTSService {

    private static final String UPLOAD_DIR = "uploads/";

    static {
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }

    /**
     * 使用Windows系统TTS生成语音
     */
    public String textToSpeech(String text) throws Exception {
        // 生成唯一文件名
        String filename = "tts_" + UUID.randomUUID().toString() + ".wav";
        String outputPath = UPLOAD_DIR + filename;

        // 清理文本中的特殊字符
        String cleanedText = text.replace("\"", "\\\"")
                .replace("'", "\\'")
                .replace("\n", " ");

        try {
            // PowerShell命令，使用System.Speech.Synthesis
            String command = String.format(
                    "powershell -Command \"Add-Type -AssemblyName System.speech; " +
                            "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                            "$speak.SetOutputToWaveFile('%s'); " +
                            "$speak.Speak('%s'); " +
                            "$speak.Dispose()\"",
                    outputPath.replace("'", "''"),
                    cleanedText
            );

            Process process = Runtime.getRuntime().exec(command);

            // 读取错误流
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));
            StringBuilder errors = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errors.append(line).append("\n");
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                File file = new File(outputPath);
                if (file.exists() && file.length() > 0) {
                    return filename;
                } else {
                    throw new Exception("语音文件生成失败");
                }
            } else {
                throw new Exception("TTS执行失败: " + errors.toString());
            }

        } catch (Exception e) {
            throw new Exception("文字转语音失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取音频文件
     */
    public byte[] getAudioFile(String filename) throws IOException {
        Path filePath = Paths.get(UPLOAD_DIR + filename);
        return Files.readAllBytes(filePath);
    }

    /**
     * 删除音频文件
     */
    public boolean deleteAudioFile(String filename) {
        File file = new File(UPLOAD_DIR + filename);
        return file.delete();
    }
}