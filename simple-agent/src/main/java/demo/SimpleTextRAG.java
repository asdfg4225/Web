package demo;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class SimpleTextRAG {

    private String knowledgeBasePath;
    private List<String> documents = new ArrayList<>();
    private Map<String, String> documentMap = new HashMap<>();

    /**
     * 初始化知识库
     */
    public void init(String knowledgeBasePath) throws IOException {
        this.knowledgeBasePath = knowledgeBasePath;
        System.out.println("📁 加载知识库: " + knowledgeBasePath);

        // 加载所有文本文件
        loadTextFiles(knowledgeBasePath);

        System.out.println("✅ 知识库加载完成，共 " + documents.size() + " 条知识");
    }

    /**
     * 加载文本文件
     */
    private void loadTextFiles(String directory) throws IOException {
        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("⚠️ 知识库目录不存在，将创建示例数据");
            createSampleKnowledge();
            return;
        }

        // 读取所有txt文件
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".txt"));
        if (files == null || files.length == 0) {
            System.out.println("⚠️ 没有找到知识库文件，将创建示例数据");
            createSampleKnowledge();
            return;
        }

        for (File file : files) {
            String content = new String(Files.readAllBytes(file.toPath()), "UTF-8");
            String id = file.getName().replace(".txt", "");
            documents.add(content);
            documentMap.put(id, content);

            System.out.println("📄 加载: " + file.getName() + " (" + content.length() + " 字符)");
        }
    }

    /**
     * 创建示例知识库
     */
    private void createSampleKnowledge() {
        // 创建一些基本的医疗知识
        String[] sampleDocs = {
                // 高血压知识
                "高血压是一种常见的心血管疾病。\n" +
                        "预防方法：\n" +
                        "1. 低盐饮食，每天摄入盐不超过6克\n" +
                        "2. 适量运动，每周至少150分钟中等强度运动\n" +
                        "3. 戒烟限酒\n" +
                        "4. 保持正常体重\n" +
                        "5. 定期测量血压\n" +
                        "治疗建议：遵医嘱服药，定期复查。",

                // 糖尿病知识
                "糖尿病饮食注意事项：\n" +
                        "1. 控制碳水化合物摄入\n" +
                        "2. 多吃蔬菜，特别是绿叶蔬菜\n" +
                        "3. 选择低GI食物\n" +
                        "4. 定时定量进餐\n" +
                        "5. 避免高糖食物和饮料\n" +
                        "6. 适当摄入优质蛋白质\n" +
                        "建议：定期监测血糖，遵医嘱用药。",

                // 心脏病急救
                "心脏病急救方法：\n" +
                        "1. 立即拨打120急救电话\n" +
                        "2. 让患者保持安静休息\n" +
                        "3. 如有硝酸甘油，舌下含服\n" +
                        "4. 如果患者昏迷，立即进行心肺复苏\n" +
                        "5. 保持空气流通\n" +
                        "注意：不要随意搬动患者。",

                // 头痛发热处理
                "头痛发热处理方法：\n" +
                        "1. 测量体温，判断发热程度\n" +
                        "2. 多喝水，保持水分\n" +
                        "3. 适当休息\n" +
                        "4. 可用物理降温（温水擦浴）\n" +
                        "5. 遵医嘱服用退热药\n" +
                        "6. 如持续高热不退，及时就医",

                // 老年人健康管理
                "老年人健康管理要点：\n" +
                        "1. 定期体检，每年至少一次全面检查\n" +
                        "2. 均衡饮食，适量补充钙和维生素D\n" +
                        "3. 适当运动，如散步、太极拳\n" +
                        "4. 保持良好心态，积极参与社交活动\n" +
                        "5. 按时服药，管理慢性疾病\n" +
                        "6. 预防跌倒，注意居家安全"
        };

        for (int i = 0; i < sampleDocs.length; i++) {
            documents.add(sampleDocs[i]);
            documentMap.put("doc_" + (i + 1), sampleDocs[i]);
        }

        // 保存为文件，便于后续使用
        saveKnowledgeToFile();
    }

    /**
     * 保存知识库到文件
     */
    private void saveKnowledgeToFile() {
        File dir = new File("knowledge_base");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String[] fileNames = {"高血压.txt", "糖尿病.txt", "心脏病.txt", "头痛发热.txt", "老年人健康.txt"};

        for (int i = 0; i < documents.size() && i < fileNames.length; i++) {
            try {
                File file = new File(dir, fileNames[i]);
                Files.write(file.toPath(), documents.get(i).getBytes("UTF-8"));
                System.out.println("💾 保存: " + file.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("保存文件失败: " + e.getMessage());
            }
        }
    }

    /**
     * 简单关键词匹配搜索
     */
    public List<String> simpleSearch(String query, int topK) {
        List<String> results = new ArrayList<>();

        // 提取查询中的关键词
        String[] keywords = extractKeywords(query);

        // 计算每个文档的匹配分数
        Map<String, Integer> scores = new HashMap<>();

        for (int i = 0; i < documents.size(); i++) {
            String doc = documents.get(i);
            int score = 0;

            // 计算关键词匹配分数
            for (String keyword : keywords) {
                if (doc.contains(keyword)) {
                    score += 3; // 完全匹配得分高
                }

                // 分词匹配（简单实现）
                for (String word : doc.split("[\\s\\p{Punct}]+")) {
                    if (word.contains(keyword) || keyword.contains(word)) {
                        score += 1;
                    }
                }
            }

            // 如果文档标题或开头包含查询，增加分数
            if (doc.startsWith(query) || doc.contains("【" + query + "】")) {
                score += 5;
            }

            if (score > 0) {
                scores.put("doc_" + (i + 1) + "|" + doc.substring(0, Math.min(50, doc.length())), score);
            }
        }

        // 按分数排序并返回topK个结果
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 提取关键词
     */
    private String[] extractKeywords(String query) {
        // 医疗相关关键词
        Set<String> medicalKeywords = new HashSet<>(Arrays.asList(
                "血压", "高血压", "低血压", "血糖", "糖尿病", "心脏", "心脏病", "心梗",
                "头痛", "偏头痛", "发热", "发烧", "咳嗽", "感冒", "流感",
                "药物", "药品", "治疗", "预防", "饮食", "运动", "锻炼",
                "检查", "体检", "症状", "诊断", "手术", "康复", "恢复",
                "老年人", "老人", "儿童", "小孩", "孕妇", "产妇",
                "急救", "抢救", "危险", "严重", "紧急", "医院", "医生"
        ));

        List<String> foundKeywords = new ArrayList<>();

        // 从查询中提取关键词
        for (String keyword : medicalKeywords) {
            if (query.contains(keyword)) {
                foundKeywords.add(keyword);
            }
        }

        // 如果没找到特定关键词，使用整个查询作为关键词
        if (foundKeywords.isEmpty()) {
            foundKeywords.add(query);
        }

        return foundKeywords.toArray(new String[0]);
    }

    /**
     * RAG检索
     */
    public String ragSearch(String query) {
        System.out.println("🔍 检索查询: " + query);

        List<String> searchResults = simpleSearch(query, 3);

        if (searchResults.isEmpty()) {
            return "没有找到相关信息。\n\n您可以尝试：\n" +
                    "1. 提供更具体的症状描述\n" +
                    "2. 查询其他相关健康主题\n" +
                    "3. 联系专业医疗人员";
        }

        StringBuilder response = new StringBuilder();
        response.append("💡 关于「").append(query).append("」的查询结果：\n\n");

        for (int i = 0; i < searchResults.size(); i++) {
            String result = searchResults.get(i);
            String[] parts = result.split("\\|", 2);
            String docId = parts[0];
            String contentPreview = parts.length > 1 ? parts[1] : "";

            // 获取完整文档内容
            String fullContent = documentMap.getOrDefault(docId.split("\\|")[0], "");
            if (fullContent.isEmpty() && i < documents.size()) {
                fullContent = documents.get(i);
            }

            response.append("【结果 ").append(i + 1).append("】\n");
            response.append(fullContent).append("\n\n");
        }

        response.append("📌 温馨提示：\n");
        response.append("1. 以上信息仅供参考，不能替代专业医疗建议\n");
        response.append("2. 如有健康问题，请及时咨询医生\n");
        response.append("3. 紧急情况请立即拨打120或前往医院\n\n");
        response.append("🕒 检索时间：").append(new java.util.Date());

        return response.toString();
    }

    /**
     * 添加新知识
     */
    public void addKnowledge(String title, String content) throws IOException {
        String fileName = title.replaceAll("[\\\\/:*?\"<>|]", "_") + ".txt";
        File file = new File("knowledge_base/" + fileName);

        String fullContent = "【" + title + "】\n" + content;
        Files.write(file.toPath(), fullContent.getBytes("UTF-8"));

        documents.add(fullContent);
        documentMap.put(fileName.replace(".txt", ""), fullContent);

        System.out.println("✅ 已添加新知识: " + title);
    }

    /**
     * 显示知识库统计
     */
    public void showStats() {
        System.out.println("\n📊 知识库统计信息:");
        System.out.println("• 知识条目: " + documents.size());
        System.out.println("• 存储路径: " + knowledgeBasePath);

        // 按主题统计
        Map<String, Integer> topicCount = new HashMap<>();
        for (String doc : documents) {
            if (doc.contains("高血压")) topicCount.put("高血压", topicCount.getOrDefault("高血压", 0) + 1);
            if (doc.contains("糖尿病")) topicCount.put("糖尿病", topicCount.getOrDefault("糖尿病", 0) + 1);
            if (doc.contains("心脏")) topicCount.put("心脏病", topicCount.getOrDefault("心脏病", 0) + 1);
            if (doc.contains("头痛") || doc.contains("发热")) topicCount.put("常见症状", topicCount.getOrDefault("常见症状", 0) + 1);
            if (doc.contains("老年")) topicCount.put("老年人健康", topicCount.getOrDefault("老年人健康", 0) + 1);
        }

        System.out.println("• 主题分布:");
        for (Map.Entry<String, Integer> entry : topicCount.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " 条");
        }
    }

    /**
     * 主方法 - 测试
     */
    public static void main(String[] args) {
        SimpleTextRAG rag = new SimpleTextRAG();

        try {
            // 1. 初始化知识库
            rag.init("knowledge_base");

            System.out.println("\n=== 简单RAG知识库系统 ===\n");

            // 2. 测试查询
            String[] testQueries = {
                    "高血压怎么预防",
                    "糖尿病饮食",
                    "心脏病急救",
                    "头痛发热怎么办",
                    "老年人健康管理"
            };

            for (String query : testQueries) {
                System.out.println("🔍 查询: " + query);
                System.out.println("📝 回答:");

                long startTime = System.currentTimeMillis();
                String response = rag.ragSearch(query);
                long endTime = System.currentTimeMillis();

                System.out.println(response);
                System.out.println("⏱️ 检索耗时: " + (endTime - startTime) + "ms");

                Thread.sleep(300);
            }

            // 3. 显示统计信息
            rag.showStats();

            // 4. 演示添加新知识
            System.out.println("\n💾 演示添加新知识...");
            rag.addKnowledge("感冒预防",
                    "感冒预防方法：\n" +
                            "1. 勤洗手，保持手部卫生\n" +
                            "2. 避免接触感冒患者\n" +
                            "3. 保持室内空气流通\n" +
                            "4. 增强免疫力，保证充足睡眠\n" +
                            "5. 天气变化时注意增减衣物");

            System.out.println("\n🔍 测试新添加的知识...");
            String newQueryResponse = rag.ragSearch("感冒预防");
            System.out.println(newQueryResponse.substring(0, Math.min(200, newQueryResponse.length())) + "...");

        } catch (Exception e) {
            System.err.println("❌ 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}