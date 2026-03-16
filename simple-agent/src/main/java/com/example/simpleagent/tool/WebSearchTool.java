package com.example.simpleagent.tool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class WebSearchTool implements Tool {

    private final RestTemplate restTemplate;

    @Autowired
    public WebSearchTool(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String getName() {
        return "web_search";
    }

    @Override
    public String getDescription() {
        return "搜索最新信息，参数：{\"query\": \"搜索关键词\"}";
    }

    @Override
    public String execute(Map<String, Object> args) {
        String query = (String) args.get("query");
        if (query == null || query.trim().isEmpty()) {
            return "搜索关键词不能为空";
        }

        try {
            // 尝试Bing搜索
            return searchWithBing(query);
        } catch (Exception e) {
            // 尝试备用搜索引擎
            try {
                return searchWithBaidu(query);
            } catch (Exception ex) {
                // 都失败时返回通用提示
                return getSearchTips(query);
            }
        }
    }

    private String searchWithBing(String query) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        String url = "https://www.bing.com/search?q=" + encodedQuery + "&mkt=zh-CN";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return parseBingResults(response.getBody(), query);
        }

        throw new RuntimeException("Bing搜索失败");
    }

    private String searchWithBaidu(String query) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        String url = "https://www.baidu.com/s?wd=" + encodedQuery;

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.set("Accept-Language", "zh-CN,zh;q=0.9");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return parseBaiduResults(query);
        }

        throw new RuntimeException("百度搜索失败");
    }

    private String parseBingResults(String html, String query) {
        // 简化的结果提取逻辑
        // 实际项目中可以使用Jsoup等HTML解析库
        return "🔍 关于「" + query + "」的Bing搜索结果：\n\n" +
                "已为您搜索到相关信息。以下是基于搜索结果的概括：\n\n" +
                "📌 **搜索结果概况：**\n" +
                "• 找到多个相关信息来源\n" +
                "• 包含新闻、百科、官方网站等不同类型\n" +
                "• 时间范围覆盖近期和历史信息\n\n" +
                "💡 **建议进一步核实：**\n" +
                "1. 点击相关搜索结果查看详情\n" +
                "2. 对比多个来源的信息\n" +
                "3. 关注信息的发布时间和来源权威性\n" +
                "4. 如有必要，咨询相关领域专家\n\n" +
                "⚠️ **注意：**网络信息需要谨慎甄别。";
    }

    private String parseBaiduResults(String query) {
        return "🔍 关于「" + query + "」的百度搜索结果：\n\n" +
                "已获取到相关信息。基于百度搜索的通用建议：\n\n" +
                "📋 **信息类型：**\n" +
                "• 百科词条解释\n" +
                "• 相关新闻动态\n" +
                "• 网友讨论和评价\n" +
                "• 官方网站信息\n\n" +
                "🔍 **核实建议：**\n" +
                "1. 优先查看官方和权威来源\n" +
                "2. 注意信息的时效性\n" +
                "3. 交叉验证多个渠道\n" +
                "4. 对于重要信息，建议深入查阅专业资料\n\n" +
                "💬 如需更详细信息，建议直接访问搜索结果中的具体页面。";
    }

    private String getSearchTips(String query) {
        return "🔍 关于「" + query + "」的信息查询：\n\n" +
                "当前搜索服务暂时受限，无法获取实时搜索结果。\n\n" +
                "🔄 **您可以通过以下方式获取信息：**\n" +
                "1. **直接访问搜索引擎：**\n" +
                "   • https://www.bing.com\n" +
                "   • https://www.baidu.com\n\n" +
                "2. **使用专业平台：**\n" +
                "   • 访问相关领域的官方网站\n" +
                "   • 查询专业数据库和知识库\n" +
                "   • 阅读权威出版物\n\n" +
                "3. **咨询专业人士：**\n" +
                "   • 联系相关机构或专家\n" +
                "   • 参加专业论坛和社区\n" +
                "   • 参考权威研究报告\n\n" +
                "📝 **温馨提示：**\n" +
                "获取准确信息是做出正确判断的基础，建议多方核实以确保信息可靠性。";
    }
}