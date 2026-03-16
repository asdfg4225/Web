package com.example.simpleagent.service;

import com.example.simpleagent.model.MedicationReminder;
import com.example.simpleagent.model.Message;
import com.example.simpleagent.model.User;
import com.example.simpleagent.tool.Tool;
import com.example.simpleagent.config.SiliconFlowConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiAgentService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SiliconFlowConfig config;
    private final ToolService toolService;
    private final UserService userService;

    @Autowired
    public AiAgentService(RestTemplate restTemplate, ObjectMapper objectMapper,
                          SiliconFlowConfig config, ToolService toolService,
                          UserService userService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.config = config;
        this.toolService = toolService;
        this.userService = userService;
    }

    public String getResponse(List<Message> messages) {
        try {
            List<Map<String, String>> messageMaps = new ArrayList<>();
            for (Message msg : messages) {
                Map<String, String> map = new HashMap<>();
                map.put("role", msg.getRole());
                map.put("content", msg.getContent());
                messageMaps.add(map);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "Qwen/Qwen2.5-7B-Instruct");
            requestBody.put("messages", messageMaps);
            requestBody.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + config.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(config.getApiUrl(), request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );

                List<?> choices = (List<?>) responseBody.get("choices");
                Map<?, ?> message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
                return (String) message.get("content");
            } else {
                return "❌ API 错误: " + response.getStatusCode();
            }
        } catch (Exception e) {
            return "❌ 请求失败: " + e.getMessage();
        }
    }

    public String callModelForTools(List<Map<String, String>> messages) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "Qwen/Qwen2.5-7B-Instruct");
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + config.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        System.out.println("🤖 发送AI请求到: " + config.getApiUrl());

        ResponseEntity<String> response = restTemplate.exchange(
                config.getApiUrl(),
                HttpMethod.POST,
                request,
                String.class
        );

        System.out.println("🤖 AI响应状态: " + response.getStatusCode());

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        }

        System.out.println("🤖 AI请求失败: " + response.getStatusCode() + " - " + response.getBody());
        throw new RuntimeException("API error: " + response.getStatusCode());
    }

    private List<Map<String, String>> convertToMapList(List<Message> messages) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Message msg : messages) {
            Map<String, String> map = new HashMap<>();
            map.put("role", msg.getRole());
            map.put("content", msg.getContent());
            result.add(map);
        }
        return result;
    }

    public String getResponseWithTools(List<Message> messages, ToolService toolService) {
        try {
            // 获取最后一条用户消息
            String lastUserMessage = messages.isEmpty() ? "" :
                    messages.get(messages.size() - 1).getContent();

            System.out.println("🔄 处理用户消息: " + lastUserMessage);

            // === 第一步：检查是否紧急情况 ===
            String emergencyCheck = checkEmergency(lastUserMessage);
            if (emergencyCheck != null) {
                System.out.println("🚨 检测到紧急情况！");
                return emergencyCheck;
            }

            // === 第二步：判断是否需要调用其他工具 ===
            List<Map<String, String>> history = convertToMapList(messages);

            String toolPrompt = buildToolPrompt(toolService, lastUserMessage);

            List<Map<String, String>> firstRound = new ArrayList<>(history);

            Map<String, String> userPromptMap = new HashMap<>();
            userPromptMap.put("role", "user");
            userPromptMap.put("content", toolPrompt);
            firstRound.add(userPromptMap);

            System.out.println("🔧 发送工具决策请求...");
            String firstResponse = callModelForTools(firstRound);
            System.out.println("🔧 工具决策响应: " + firstResponse);

            // 清理响应内容，提取JSON
            firstResponse = firstResponse.trim();
            if (firstResponse.startsWith("```json")) {
                firstResponse = firstResponse.substring(7, firstResponse.length() - 3).trim();
            } else if (firstResponse.startsWith("```")) {
                firstResponse = firstResponse.substring(3, firstResponse.length() - 3).trim();
            }

            // 解析 JSON
            try {
                JsonNode decision = objectMapper.readTree(firstResponse);

                if (decision.has("need_tool") && decision.path("need_tool").asBoolean()) {
                    String toolName = decision.path("tool_name").asText();
                    JsonNode argsNode = decision.path("tool_args");
                    Map<String, Object> args = objectMapper.convertValue(argsNode, Map.class);

                    System.out.println("🛠️ 准备调用工具: " + toolName + ", 参数: " + args);

                    Tool tool = toolService.getTool(toolName);
                    if (tool != null) {
                        String toolResult = tool.execute(args);
                        System.out.println("🛠️ 工具执行结果: " + toolResult);

                        // === 第三步：结合工具结果生成最终回答 ===
                        List<Map<String, String>> finalHistory = new ArrayList<>(history);

                        Map<String, String> systemMap = new HashMap<>();
                        systemMap.put("role", "system");
                        systemMap.put("content", "用户需要的信息已经获取到，以下是相关信息：\n" + toolResult);
                        finalHistory.add(systemMap);

                        Map<String, String> finalUserMap = new HashMap<>();
                        finalUserMap.put("role", "user");
                        finalUserMap.put("content", "请根据以上信息，用友好、关心、专业的语气回答用户的问题：" + lastUserMessage);
                        finalHistory.add(finalUserMap);

                        System.out.println("🤖 生成最终回答...");
                        String finalResponse = callModelForTools(finalHistory);
                        System.out.println("🤖 最终回答生成完成");

                        return finalResponse;
                    } else {
                        return "❌ 找不到工具: " + toolName + "，请确认工具名称是否正确。";
                    }
                } else if (decision.has("answer")) {
                    return decision.path("answer").asText();
                } else {
                    // 如果没有明确的决策，直接调用模型
                    System.out.println("🤖 直接调用模型回答...");
                    return callModelForTools(history);
                }
            } catch (Exception e) {
                System.out.println("❌ 解析工具决策失败: " + e.getMessage());
                // 如果解析失败，直接调用模型
                return callModelForTools(history);
            }

        } catch (Exception e) {
            System.out.println("❌ 系统错误: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();

            return "抱歉，系统暂时遇到问题。\n" +
                    "您可以尝试：\n" +
                    "1. 稍后再试\n" +
                    "2. 直接联系社区服务中心\n" +
                    "3. 拨打相关服务热线\n\n" +
                    "错误详情：" + e.getMessage();
        }
    }

    // 新增：检查紧急情况的方法
    private String checkEmergency(String userMessage) {
        try {
            Tool emergencyTool = toolService.getTool("emergency_check");
            if (emergencyTool == null) {
                System.out.println("⚠️ 紧急情况工具未找到");
                return null;
            }

            Map<String, Object> args = new HashMap<>();
            args.put("query", userMessage);

            String emergencyResult = emergencyTool.execute(args);
            JsonNode resultNode = objectMapper.readTree(emergencyResult);

            if (resultNode.path("is_emergency").asBoolean()) {
                // 检测到紧急情况，立即返回紧急指导
                System.out.println("🚨 检测到紧急情况: " + resultNode.path("emergency_type").asText());

                return "🚨 **紧急情况警报！** 🚨\n\n" +
                        "**检测到紧急医疗情况：** " + resultNode.path("emergency_type").asText() + "\n\n" +
                        "**🆘 立即行动指南：**\n" +
                        "1. 📞 **立即拨打 120 急救电话**\n" +
                        "2. 👨‍⚕️ 保持冷静，清晰说明症状和地址\n" +
                        "3. 🚪 打开房门，方便救援人员进入\n" +
                        "4. 💊 准备好医保卡、身份证和常用药物\n\n" +
                        "**🧑‍⚕️ 急救措施：**\n" +
                        "• " + resultNode.path("immediate_action").asText().replace("\n", "\n• ") + "\n\n" +
                        "**🔔 重要提醒：**\n" +
                        "• 不要随意移动患者\n" +
                        "• 保持患者呼吸道通畅\n" +
                        "• 记录症状开始时间\n" +
                        "• 通知家人或邻居协助\n\n" +
                        "💗 **生命至上，请立即行动！**";
            }
        } catch (Exception e) {
            System.out.println("❌ 紧急情况检测失败: " + e.getMessage());
        }
        return null;
    }

    private String buildToolPrompt(ToolService toolService, String userMessage) {
        return "你是一个面向社区老年人的AI健康助手，需要严谨、准确、贴心地回答问题。\n\n" +
                "当用户询问以下信息时，必须调用相应工具：\n" +
                " 1. 当前时间 -> 调用 get_current_time\n" +
                " 2. 天气信息 -> 调用 get_weather，参数需要包含城市名\n" +
                " 3. 附近医院 -> 调用 search_hospital，参数需要包含详细地址\n" +
                " 4. 最新信息 -> 调用 web_search，参数需要包含搜索关键词\n" +
                " 5. 用药提醒 -> 调用 set_medication_reminder，参数需要包含药品名和时间\n\n" +
                "**注意：如果用户提到心脏病、中风、胸痛、呼吸困难等紧急医疗情况，系统会自动处理，你不需要调用工具。**\n\n" +
                "可用工具：\n" +
                toolService.getToolsDescription() + "\n\n" +
                "用户问题：「" + userMessage + "」\n\n" +
                "请严格按照以下JSON格式回复，不要有任何额外文字：\n" +
                "如果需要调用工具：\n" +
                "{\n" +
                "  \"need_tool\": true,\n" +
                "  \"tool_name\": \"工具名称\",\n" +
                "  \"tool_args\": {\n" +
                "    \"参数名1\": \"参数值1\",\n" +
                "    \"参数名2\": \"参数值2\"\n" +
                "  }\n" +
                "}\n\n" +
                "如果不需要调用工具，直接回答：\n" +
                "{\n" +
                "  \"need_tool\": false,\n" +
                "  \"answer\": \"你的回答内容\"\n" +
                "}\n\n" +
                "注意：\n" +
                "1. 涉及具体时间、地点、天气、最新资讯的问题必须调用工具\n" +
                "2. 参数值必须是字符串类型\n" +
                "3. 保持回答专业、温暖、贴心";
    }

    // 在AiAgentService中添加以下方法
    // 新方法：处理带历史记录的多轮对话
    public String getResponseWithToolsAndContext(List<Message> messages, ToolService toolService, Long userId) {
        try {
            // 获取用户健康档案
            Map<String, Object> healthSummary = userService.getHealthSummary(userId);
            User user = (User) healthSummary.get("user");

            // 构建包含用户上下文的prompt
            String userContext = buildUserContext(user, healthSummary);

            // 获取最后一条用户消息（如果有的话）
            String lastUserMessage = "";
            if (!messages.isEmpty()) {
                // 找到最后一条用户消息
                for (int i = messages.size() - 1; i >= 0; i--) {
                    if ("user".equals(messages.get(i).getRole())) {
                        lastUserMessage = messages.get(i).getContent();
                        break;
                    }
                }
            }

            System.out.println("🔄 处理用户消息（带上下文和历史记录）: " + lastUserMessage);
            System.out.println("📚 历史记录条数: " + messages.size());

            // 检查紧急情况（使用用户信息增强）
            String emergencyCheck = checkEmergencyWithContext(lastUserMessage, user);
            if (emergencyCheck != null) {
                return emergencyCheck;
            }

            // 将消息转换为API需要的格式
            List<Map<String, String>> apiMessages = new ArrayList<>();

            // 添加系统消息（用户上下文）
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", userContext + "\n\n请根据用户历史对话和健康档案信息回答用户问题。");
            apiMessages.add(systemMessage);

            // 添加历史对话消息（最多保留10轮对话）
            int historyLimit = Math.min(messages.size(), 20); // 最多20条历史消息
            for (int i = Math.max(0, messages.size() - historyLimit); i < messages.size(); i++) {
                Message msg = messages.get(i);
                Map<String, String> messageMap = new HashMap<>();
                messageMap.put("role", msg.getRole());
                messageMap.put("content", msg.getContent());
                apiMessages.add(messageMap);
            }

            // 构建工具提示
            String toolPrompt = buildToolPrompt(toolService, lastUserMessage);

            List<Map<String, String>> firstRound = new ArrayList<>(apiMessages);

            Map<String, String> userPromptMap = new HashMap<>();
            userPromptMap.put("role", "user");
            userPromptMap.put("content", toolPrompt);
            firstRound.add(userPromptMap);

            System.out.println("🔧 发送工具决策请求（带历史记录）...");
            String firstResponse = callModelForTools(firstRound);
            System.out.println("🔧 工具决策响应: " + firstResponse);

            // 清理响应内容，提取JSON
            firstResponse = firstResponse.trim();
            if (firstResponse.startsWith("```json")) {
                firstResponse = firstResponse.substring(7, firstResponse.length() - 3).trim();
            } else if (firstResponse.startsWith("```")) {
                firstResponse = firstResponse.substring(3, firstResponse.length() - 3).trim();
            }

            // 解析 JSON
            try {
                JsonNode decision = objectMapper.readTree(firstResponse);

                if (decision.has("need_tool") && decision.path("need_tool").asBoolean()) {
                    String toolName = decision.path("tool_name").asText();
                    JsonNode argsNode = decision.path("tool_args");
                    Map<String, Object> args = objectMapper.convertValue(argsNode, Map.class);

                    System.out.println("🛠️ 准备调用工具: " + toolName + ", 参数: " + args);

                    Tool tool = toolService.getTool(toolName);
                    if (tool != null) {
                        String toolResult = tool.execute(args);
                        System.out.println("🛠️ 工具执行结果: " + toolResult);

                        // 结合工具结果生成最终回答
                        List<Map<String, String>> finalMessages = new ArrayList<>(apiMessages);

                        Map<String, String> systemMap = new HashMap<>();
                        systemMap.put("role", "system");
                        systemMap.put("content", "用户需要的信息已经获取到，以下是相关信息：\n" + toolResult);
                        finalMessages.add(systemMap);

                        Map<String, String> finalUserMap = new HashMap<>();
                        finalUserMap.put("role", "user");
                        finalUserMap.put("content", "请根据以上信息和历史对话，用友好、关心、专业的语气回答用户的问题：" + lastUserMessage);
                        finalMessages.add(finalUserMap);

                        System.out.println("🤖 生成最终回答（带历史记录）...");
                        String finalResponse = callModelForTools(finalMessages);
                        System.out.println("🤖 最终回答生成完成");

                        return finalResponse;
                    } else {
                        return "❌ 找不到工具: " + toolName + "，请确认工具名称是否正确。";
                    }
                } else if (decision.has("answer")) {
                    return decision.path("answer").asText();
                } else {
                    // 如果没有明确的决策，直接调用模型
                    System.out.println("🤖 直接调用模型回答（带历史记录）...");

                    // 添加提示，让模型考虑历史对话
                    Map<String, String> finalPrompt = new HashMap<>();
                    finalPrompt.put("role", "user");
                    finalPrompt.put("content", "请根据我们的历史对话，继续回答：" + lastUserMessage);
                    apiMessages.add(finalPrompt);

                    return callModelForTools(apiMessages);
                }
            } catch (Exception e) {
                System.out.println("❌ 解析工具决策失败: " + e.getMessage());
                // 如果解析失败，直接调用模型
                return callModelForTools(apiMessages);
            }

        } catch (Exception e) {
            System.out.println("❌ 系统错误: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();

            return "抱歉，系统暂时遇到问题。\n" +
                    "您可以尝试：\n" +
                    "1. 稍后再试\n" +
                    "2. 直接联系社区服务中心\n" +
                    "3. 拨打相关服务热线\n\n" +
                    "错误详情：" + e.getMessage();
        }
    }

    // 新增方法：获取对话摘要
    public String generateConversationSummary(List<Message> messages) {
        try {
            if (messages == null || messages.isEmpty()) {
                return "暂无对话历史";
            }

            // 构建摘要提示
            List<Map<String, String>> apiMessages = new ArrayList<>();

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "请为以下对话生成一个简洁的摘要，突出主要问题和解决方案：");
            apiMessages.add(systemMessage);

            // 添加对话历史
            for (Message msg : messages) {
                Map<String, String> messageMap = new HashMap<>();
                messageMap.put("role", msg.getRole());
                messageMap.put("content", msg.getContent());
                apiMessages.add(messageMap);
            }

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "请生成对话摘要：");
            apiMessages.add(userMessage);

            // 调用模型生成摘要
            return callModelForTools(apiMessages);

        } catch (Exception e) {
            System.err.println("生成对话摘要失败: " + e.getMessage());
            return "生成对话摘要失败";
        }
    }

    // 新增方法：从历史中提取关键信息
    public Map<String, String> extractKeyInfoFromHistory(List<Message> messages) {
        Map<String, String> keyInfo = new HashMap<>();

        if (messages == null || messages.isEmpty()) {
            return keyInfo;
        }

        // 提取可能的症状、药物、时间等信息
        StringBuilder symptoms = new StringBuilder();
        StringBuilder medications = new StringBuilder();
        StringBuilder concerns = new StringBuilder();

        for (Message msg : messages) {
            String content = msg.getContent().toLowerCase();

            // 提取症状关键词
            if (content.contains("头疼") || content.contains("头痛")) {
                symptoms.append("头痛、");
            }
            if (content.contains("头晕") || content.contains("眩晕")) {
                symptoms.append("头晕、");
            }
            if (content.contains("发烧") || content.contains("发热")) {
                symptoms.append("发烧、");
            }
            if (content.contains("咳嗽")) {
                symptoms.append("咳嗽、");
            }

            // 提取药物关键词
            if (content.contains("药") && !content.contains("医院") && !content.contains("药店")) {
                // 简单提取药物信息
                medications.append(content.substring(Math.max(0, content.indexOf("药") - 10),
                        Math.min(content.length(), content.indexOf("药") + 10))).append("、");
            }

            // 提取关注点
            if (content.contains("担心") || content.contains("害怕") || content.contains("焦虑")) {
                concerns.append(content).append("、");
            }
        }

        if (symptoms.length() > 0) {
            keyInfo.put("symptoms", symptoms.substring(0, symptoms.length() - 1));
        }
        if (medications.length() > 0) {
            keyInfo.put("medications", medications.substring(0, medications.length() - 1));
        }
        if (concerns.length() > 0) {
            keyInfo.put("concerns", concerns.substring(0, concerns.length() - 1));
        }

        return keyInfo;
    }

    private String buildUserContext(User user, Map<String, Object> healthSummary) {
        StringBuilder context = new StringBuilder();
        context.append("【用户健康档案】\n");
        context.append("姓名：").append(user.getRealName()).append("\n");
        context.append("年龄：").append(user.getAge()).append("\n");
        context.append("性别：").append(user.getGender()).append("\n");

        if (user.getChronicDiseases() != null && !user.getChronicDiseases().isEmpty()) {
            context.append("慢性病史：").append(user.getChronicDiseases()).append("\n");
        }

        if (user.getAllergies() != null && !user.getAllergies().isEmpty()) {
            context.append("过敏史：").append(user.getAllergies()).append("\n");
        }

        if (user.getMedications() != null && !user.getMedications().isEmpty()) {
            context.append("当前用药：").append(user.getMedications()).append("\n");
        }

        List<MedicationReminder> reminders = (List<MedicationReminder>) healthSummary.get("activeReminders");
        if (reminders != null && !reminders.isEmpty()) {
            context.append("用药提醒：\n");
            for (MedicationReminder reminder : reminders) {
                context.append("- ").append(reminder.getMedicationName())
                        .append("，").append(reminder.getDosage())
                        .append("，").append(reminder.getFrequency())
                        .append("，").append(reminder.getTimeOfDay()).append("\n");
            }
        }

        return context.toString();
    }

    private String checkEmergencyWithContext(String userMessage, User user) {
        // 原有紧急情况检查逻辑，但使用用户信息
        String emergencyResult = checkEmergency(userMessage);

        // 如果检测到紧急情况，添加用户联系方式
        if (emergencyResult != null && emergencyResult.contains("🚨")) {
            // 添加用户联系方式到紧急指导中
            StringBuilder enhancedEmergency = new StringBuilder(emergencyResult);
            enhancedEmergency.append("\n\n**📞 用户联系方式：**\n");
            enhancedEmergency.append("• 姓名：").append(user.getRealName()).append("\n");
            if (user.getPhone() != null) {
                enhancedEmergency.append("• 电话：").append(user.getPhone()).append("\n");
            }
            if (user.getEmergencyContact() != null && user.getEmergencyPhone() != null) {
                enhancedEmergency.append("• 紧急联系人：").append(user.getEmergencyContact())
                        .append(" (").append(user.getEmergencyPhone()).append(")\n");
            }
            if (user.getAddress() != null) {
                enhancedEmergency.append("• 地址：").append(user.getAddress()).append("\n");
            }

            return enhancedEmergency.toString();
        }

        return emergencyResult;
    }
}