package com.example.simpleagent.utils;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

public class FileUtils {

    // 上传目录
    private static final String UPLOAD_DIR = "uploads/";

    static {
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }

    /**
     * 保存上传的文件
     */
    public static String saveUploadedFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;

        Path filePath = Paths.get(UPLOAD_DIR + uniqueFilename);
        Files.copy(file.getInputStream(), filePath);

        return filePath.toString();
    }

    /**
     * 将文件转换为Base64
     */
    public static String fileToBase64(String filePath) throws IOException {
        File file = new File(filePath);
        byte[] fileContent = Files.readAllBytes(file.toPath());
        return Base64.getEncoder().encodeToString(fileContent);
    }

    /**
     * 检查是否是支持的图片格式
     */
    public static boolean isSupportedImage(String filename) {
        String[] supported = {".jpg", ".jpeg", ".png", ".bmp", ".gif", ".webp"};
        String lower = filename.toLowerCase();
        for (String ext : supported) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    /**
     * 检查是否是支持的音频格式
     */
    public static boolean isSupportedAudio(String filename) {
        String[] supported = {".mp3", ".wav", ".m4a", ".flac", ".ogg"};
        String lower = filename.toLowerCase();
        for (String ext : supported) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "tmp";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * 删除文件
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        return file.delete();
    }

    /**
     * 清理过期文件（超过1小时）
     */
    public static void cleanupExpiredFiles() {
        File uploadDir = new File(UPLOAD_DIR);
        File[] files = uploadDir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (System.currentTimeMillis() - file.lastModified() > 3600000) {
                    file.delete();
                }
            }
        }
    }
}