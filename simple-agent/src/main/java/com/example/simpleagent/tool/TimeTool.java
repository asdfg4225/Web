package com.example.simpleagent.tool;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class TimeTool implements Tool {
    @Override
    public String getName() {
        return "get_current_time";
    }

    @Override
    public String getDescription() {
        return "获取当前系统时间，无需参数";
    }

    @Override
    public String execute(Map<String, Object> args) {
        LocalDateTime now = LocalDateTime.now();
        String formatted = now.format(DateTimeFormatter.ofPattern("现在的时间是yyyy年MM月dd日 HH:mm:ss"));
        return formatted;
    }
}