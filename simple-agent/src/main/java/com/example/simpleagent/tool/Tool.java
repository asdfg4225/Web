// src/main/java/com/example/simpleagent/tool/Tool.java
package com.example.simpleagent.tool;

import java.util.Map;

public interface Tool {
    String getName(); // 工具名，如 "weather"
    String getDescription(); // 描述，用于 Prompt
    String execute(Map<String, Object> args); // 执行逻辑
}