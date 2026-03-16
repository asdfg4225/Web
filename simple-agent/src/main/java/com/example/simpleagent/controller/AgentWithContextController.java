package com.example.simpleagent.controller;

import com.example.simpleagent.model.Message;
import com.example.simpleagent.service.AiAgentService;
import com.example.simpleagent.service.ToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/agent")
public class AgentWithContextController {

    @Autowired
    private AiAgentService aiAgentService;

    @Autowired
    private ToolService toolService;

    // 存储用户对话历史，键为用户ID，值为消息列表
    private final Map<Long, List<Message>> conversationHistory = new ConcurrentHashMap<>();

    // 最大历史记录数
    private static final int MAX_HISTORY_SIZE = 20;

    // 带用户上下文的对话（支持多轮对话）
    @PostMapping("/chat-with-context")
    public ResponseEntity<?> chatWithContext(@RequestBody Map<String, Object> requestData,
                                             HttpSession session) {
        // 从session获取用户ID
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            // 尝试从请求中获取
            if (requestData.containsKey("userId")) {
                Object userIdObj = requestData.get("userId");
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    try {
                        userId = Long.parseLong((String) userIdObj);
                    } catch (NumberFormatException e) {
                        // 忽略格式错误
                    }
                }
            }
        }

        if (userId == null) {
            return ResponseEntity.status(401).body("请先登录");
        }

        try {
            // 获取当前消息
            Object messagesObj = requestData.get("messages");
            List<Message> currentMessages = new ArrayList<>();

            if (messagesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rawMessages = (List<Map<String, Object>>) messagesObj;

                for (Map<String, Object> rawMsg : rawMessages) {
                    Message msg = new Message();
                    msg.setRole((String) rawMsg.get("role"));
                    msg.setContent((String) rawMsg.get("content"));
                    currentMessages.add(msg);
                }
            }

            if (currentMessages.isEmpty()) {
                return ResponseEntity.badRequest().body("消息不能为空");
            }

            // 获取对话历史
            List<Message> history = conversationHistory.getOrDefault(userId, new ArrayList<>());

            // 将当前消息添加到历史记录（只添加用户消息，助手消息由AI生成后添加）
            Message userMessage = currentMessages.get(currentMessages.size() - 1);
            if ("user".equals(userMessage.getRole())) {
                history.add(userMessage);
            }

            System.out.println("🔍 用户ID: " + userId + ", 历史记录数: " + history.size());

            // 调用AI服务，传入历史记录
            String response = aiAgentService.getResponseWithToolsAndContext(
                    history, toolService, userId);

            // 将AI回复添加到历史记录
            Message assistantMessage = new Message("assistant", response);
            history.add(assistantMessage);

            // 限制历史记录大小，保留最近的MAX_HISTORY_SIZE条
            if (history.size() > MAX_HISTORY_SIZE) {
                history = history.subList(history.size() - MAX_HISTORY_SIZE, history.size());
            }

            // 保存更新后的历史记录
            conversationHistory.put(userId, history);

            // 返回响应
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("response", response);
            responseData.put("history_size", history.size());
            responseData.put("user_id", userId);

            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("对话失败：" + e.getMessage());
        }
    }

    // 获取用户的对话历史
    @GetMapping("/history/{userId}")
    public ResponseEntity<?> getConversationHistory(@PathVariable Long userId,
                                                    HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("userId");

        // 验证权限
        if (currentUserId == null || !currentUserId.equals(userId)) {
            return ResponseEntity.status(403).body("无权访问此用户的对话历史");
        }

        List<Message> history = conversationHistory.get(userId);
        if (history == null) {
            history = new ArrayList<>();
        }

        return ResponseEntity.ok(history);
    }

    // 清除用户的对话历史
    @DeleteMapping("/history/{userId}/clear")
    public ResponseEntity<?> clearConversationHistory(@PathVariable Long userId,
                                                      HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("userId");

        // 验证权限
        if (currentUserId == null || !currentUserId.equals(userId)) {
            return ResponseEntity.status(403).body("无权清除此用户的对话历史");
        }

        conversationHistory.remove(userId);
        return ResponseEntity.ok("对话历史已清除");
    }

    // 获取所有用户的对话历史（仅限管理员）
    @GetMapping("/admin/histories")
    public ResponseEntity<?> getAllConversationHistories(HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("userId");

        // 这里可以添加管理员权限检查
        // 简单实现：只允许特定用户查看所有历史
        if (currentUserId == null || currentUserId != 1L) { // 假设用户ID 1 是管理员
            return ResponseEntity.status(403).body("无权查看所有对话历史");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total_users", conversationHistory.size());
        result.put("histories", conversationHistory);

        return ResponseEntity.ok(result);
    }

    // 继续对话的便捷接口
    @PostMapping("/continue-chat")
    public ResponseEntity<?> continueChat(@RequestBody Map<String, String> request,
                                          HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("请先登录");
        }

        String userMessage = request.get("message");
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("消息不能为空");
        }

        try {
            // 获取对话历史
            List<Message> history = conversationHistory.getOrDefault(userId, new ArrayList<>());

            // 添加用户消息到历史
            history.add(new Message("user", userMessage.trim()));

            // 调用AI服务
            String response = aiAgentService.getResponseWithToolsAndContext(
                    history, toolService, userId);

            // 添加AI回复到历史
            history.add(new Message("assistant", response));

            // 限制历史记录大小
            if (history.size() > MAX_HISTORY_SIZE) {
                history = history.subList(history.size() - MAX_HISTORY_SIZE, history.size());
            }

            // 保存更新后的历史记录
            conversationHistory.put(userId, history);

            // 返回响应
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("response", response);
            responseData.put("history_size", history.size());

            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("对话失败：" + e.getMessage());
        }
    }

    // 重置对话（开始新对话）
    @PostMapping("/reset-conversation")
    public ResponseEntity<?> resetConversation(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("请先登录");
        }

        conversationHistory.remove(userId);

        // 添加系统消息，表示开始新对话
        List<Message> newHistory = new ArrayList<>();
        newHistory.add(new Message("system", "开始新的对话"));
        conversationHistory.put(userId, newHistory);

        return ResponseEntity.ok("对话已重置，开始新的对话");
    }

    // 获取对话摘要（最近几条消息）
    @GetMapping("/summary")
    public ResponseEntity<?> getConversationSummary(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("请先登录");
        }

        List<Message> history = conversationHistory.get(userId);
        if (history == null || history.isEmpty()) {
            return ResponseEntity.ok("暂无对话历史");
        }

        // 只返回最近5条消息作为摘要
        int summarySize = Math.min(5, history.size());
        List<Message> summary = history.subList(history.size() - summarySize, history.size());

        Map<String, Object> result = new HashMap<>();
        result.put("total_messages", history.size());
        result.put("summary", summary);
        result.put("last_active", new Date());

        return ResponseEntity.ok(result);
    }
}