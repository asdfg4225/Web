package com.example.simpleagent.service;

import com.example.simpleagent.tool.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ToolService {

    private final Map<String, Tool> tools = new HashMap<>();

    @Autowired
    public ToolService(TimeTool timeTool,
                       WeatherTool weatherTool,
                       HospitalSearchTool hospitalSearchTool,
                       WebSearchTool webSearchTool,
                       MedicationReminderTool medicationReminderTool,
                       EmergencyTool emergencyTool) {  // 添加 EmergencyTool
        // 注册所有工具
        System.out.println("🔧 开始注册工具...");

        try {
            registerTool(timeTool);
            System.out.println("✅ 注册时间工具: " + timeTool.getName());
        } catch (Exception e) {
            System.out.println("❌ 注册时间工具失败: " + e.getMessage());
        }

        try {
            registerTool(weatherTool);
            System.out.println("✅ 注册天气工具: " + weatherTool.getName());
        } catch (Exception e) {
            System.out.println("❌ 注册天气工具失败: " + e.getMessage());
        }

        try {
            registerTool(hospitalSearchTool);
            System.out.println("✅ 注册医院搜索工具: " + hospitalSearchTool.getName());
        } catch (Exception e) {
            System.out.println("❌ 注册医院搜索工具失败: " + e.getMessage());
        }

        try {
            registerTool(webSearchTool);
            System.out.println("✅ 注册网络搜索工具: " + webSearchTool.getName());
        } catch (Exception e) {
            System.out.println("❌ 注册网络搜索工具失败: " + e.getMessage());
        }

        try {
            registerTool(medicationReminderTool);
            System.out.println("✅ 注册用药提醒工具: " + medicationReminderTool.getName());
        } catch (Exception e) {
            System.out.println("❌ 注册用药提醒工具失败: " + e.getMessage());
        }

        try {
            registerTool(emergencyTool);  // 注册紧急情况检测工具
            System.out.println("✅ 注册紧急情况检测工具: " + emergencyTool.getName());
        } catch (Exception e) {
            System.out.println("❌ 注册紧急情况检测工具失败: " + e.getMessage());
        }

        System.out.println("🔧 工具注册完成，共注册: " + tools.size() + " 个工具");
    }

    private void registerTool(Tool tool) {
        tools.put(tool.getName(), tool);
    }

    public Tool getTool(String name) {
        Tool tool = tools.get(name);
        if (tool == null) {
            System.out.println("❌ 找不到工具: " + name + ", 可用工具: " + tools.keySet());
        }
        return tool;
    }

    public String getToolsDescription() {
        StringBuilder sb = new StringBuilder();
        for (Tool tool : tools.values()) {
            sb.append("- ").append(tool.getName())
                    .append(": ").append(tool.getDescription())
                    .append("\n");
        }
        return sb.toString();
    }
}