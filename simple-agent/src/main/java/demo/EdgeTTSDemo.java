package demo;

import java.io.*;
import java.nio.file.*;

public class EdgeTTSDemo {

    /**
     * 使用本地Edge浏览器进行TTS
     */
    public void textToSpeechWithEdge(String text, String outputPath) throws Exception {
        // 创建临时脚本文件
        try {
            // 更简单的PowerShell命令
            String command = String.format(
                    "powershell -Command \"Add-Type -AssemblyName System.speech; " +
                            "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                            "$speak.SetOutputToWaveFile('%s'); " +
                            "$speak.Speak('%s'); " +
                            "$speak.Dispose()\"",
                    outputPath.replace("'", "''"),
                    text.replace("'", "''").replace("\"", "\\\"")
            );

            Process process = Runtime.getRuntime().exec(command);

            // 读取错误流
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                System.err.println("错误: " + errorLine);
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("✅ Edge TTS生成成功");
            } else {
                System.err.println("Edge TTS执行失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            EdgeTTSDemo demo = new EdgeTTSDemo();

            String text = "您好！我是智能语音助手。今天天气很好，祝您心情愉快！";
            String outputPath = "edge_output.wav";

            System.out.println("使用Edge TTS生成语音...");
            demo.textToSpeechWithEdge(text, outputPath);

            File file = new File(outputPath);
            if (file.exists()) {
                System.out.println("文件大小: " + file.length() + " 字节");

                // 播放音频
                javax.sound.sampled.AudioInputStream audioStream =
                        javax.sound.sampled.AudioSystem.getAudioInputStream(file);
                javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();
                Thread.sleep(clip.getMicrosecondLength() / 1000);
                clip.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}