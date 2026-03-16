package com.example.simpleagent.tool;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MedicationReminderTool implements Tool {

    @Override
    public String getName() {
        return "set_medication_reminder";
    }

    @Override
    public String getDescription() {
        return "设置用药提醒，参数：{\"medication\": \"药品名\", \"time\": \"时间描述如'早上8点'\"}";
    }

    @Override
    public String execute(Map<String, Object> args) {
        String medication = (String) args.get("medication");
        String timeDesc = (String) args.get("time");

        if (medication == null || timeDesc == null) {
            return "❌ 参数缺失：请提供 medication 和 time";
        }

        // 简单提取小时（支持“早上8点”、“9点”等）
        int hour = 8;
        Pattern pattern = Pattern.compile("(\\d{1,2})[点时]");
        Matcher matcher = pattern.matcher(timeDesc);
        if (matcher.find()) {
            hour = Integer.parseInt(matcher.group(1));
            if (hour > 12 && timeDesc.contains("早上")) hour -= 12;
            if (hour <= 12 && timeDesc.contains("下午")) hour += 12;
        }

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime remindTime = tomorrow.atTime(hour, 0);
        String timeStr = remindTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return "✅ 已为您设置提醒：\n药品：" + medication + "\n时间：" + timeStr;
    }
}