package com.example.simpleagent.tool;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

@Component
public class EmergencyTool implements Tool {

    private static final Pattern EMERGENCY_PATTERN = Pattern.compile(
            "(心脏病|心梗|心肌梗塞|心绞痛|中风|脑梗|脑出血|昏迷|晕倒|晕厥|呼吸困难|气喘|窒息|哮喘发作|" +
                    "胸痛|胸闷|心悸|大出血|流血不止|骨折|摔伤|严重跌倒|烫伤|烧伤|化学品灼伤|中毒|食物中毒|" +
                    "药物过量|抽搐|癫痫发作|痉挛|高热惊厥|急腹症|剧烈腹痛|急性过敏|过敏性休克|" +
                    "视力突然丧失|言语不清|面瘫|肢体无力|失去意识|意识模糊|呕血|咯血|便血|尿血)"
    );

    @Override
    public String getName() {
        return "emergency_check";
    }

    @Override
    public String getDescription() {
        return "检查是否包含紧急医疗关键词，参数：{\"query\": \"用户输入\"}";
    }

    @Override
    public String execute(Map<String, Object> args) {
        String query = (String) args.get("query");
        if (query == null || query.trim().isEmpty()) {
            return "{\"is_emergency\": false}";
        }

        String lowerQuery = query.toLowerCase();
        boolean isEmergency = EMERGENCY_PATTERN.matcher(lowerQuery).find();

        if (isEmergency) {
            // 判断紧急类型
            String emergencyType = "医疗紧急情况";
            String specificAdvice = getSpecificEmergencyAdvice(lowerQuery);

            return String.format("{\n" +
                            "  \"is_emergency\": true,\n" +
                            "  \"emergency_type\": \"%s\",\n" +
                            "  \"immediate_action\": \"%s\",\n" +
                            "  \"specific_advice\": \"%s\"\n" +
                            "}",
                    emergencyType,
                    getImmediateAction(lowerQuery),
                    specificAdvice);
        } else {
            return "{\"is_emergency\": false}";
        }
    }

    private String getImmediateAction(String query) {
        if (query.contains("心脏病") || query.contains("心梗") || query.contains("胸痛")) {
            return "1. 立即拨打120\\n2. 让患者保持安静休息\\n3. 如有硝酸甘油，舌下含服\\n4. 解开紧身衣物";
        } else if (query.contains("中风") || query.contains("脑梗") || query.contains("面瘫")) {
            return "1. 立即拨打120\\n2. 记录症状开始时间\\n3. 让患者平躺，头偏向一侧\\n4. 不要给患者喂食任何东西";
        } else if (query.contains("呼吸困难") || query.contains("窒息") || query.contains("哮喘")) {
            return "1. 立即拨打120\\n2. 帮助患者保持坐姿\\n3. 如有哮喘喷雾剂立即使用\\n4. 保持空气流通";
        } else if (query.contains("大出血") || query.contains("流血")) {
            return "1. 立即拨打120\\n2. 用干净布直接压迫伤口\\n3. 抬高出血部位（如果可能）\\n4. 不要随意移动患者";
        } else if (query.contains("中毒")) {
            return "1. 立即拨打120\\n2. 保留呕吐物或毒物容器\\n3. 不要催吐（除非医生指示）\\n4. 保持呼吸道通畅";
        } else {
            return "1. 立即拨打120急救电话\\n2. 通知家人或邻居\\n3. 准备好医保卡和身份证\\n4. 记录症状发生时间\\n5. 不要随意移动患者";
        }
    }

    private String getSpecificEmergencyAdvice(String query) {
        if (query.contains("心脏病") || query.contains("胸痛")) {
            return "注意观察是否伴有出汗、恶心、肩背痛等症状。让患者立即停止活动。";
        } else if (query.contains("中风")) {
            return "记住FAST原则：面部不对称、手臂无力、言语不清、立即送医。溶栓治疗有黄金时间窗。";
        } else if (query.contains("骨折")) {
            return "不要尝试自行复位。用硬板固定受伤部位。冰敷减轻肿胀。";
        } else if (query.contains("烫伤")) {
            return "立即用流动冷水冲洗15-20分钟。不要涂抹牙膏、酱油等。用干净纱布覆盖。";
        } else if (query.contains("抽搐")) {
            return "移开周围危险物品。不要往嘴里塞东西。记录抽搐持续时间。发作结束后让患者侧卧。";
        }
        return "保持冷静，等待专业救援人员到来。";
    }
}