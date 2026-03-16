package com.example.simpleagent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class HospitalSearchTool implements Tool {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String amapKey;
    private String localIp = "";

    @Autowired
    public HospitalSearchTool(@Qualifier("gzipRestTemplate") RestTemplate restTemplate,
                              ObjectMapper objectMapper,
                              @Value("${amap.api.key}") String amapKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.amapKey = amapKey;

        try {
            this.localIp = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            this.localIp = "未知";
        }
    }

    @Override
    public String getName() {
        return "search_hospital";
    }

    @Override
    public String getDescription() {
        return "在指定地址附近搜索医院，参数：{\"address\": \"详细地址\"}";
    }

    @Override
    public String execute(Map<String, Object> args) {
        String address = (String) args.get("address");
        if (address == null || address.trim().isEmpty()) {
            return "❌ 地址不能为空";
        }

        try {
            String apiResult = callAmapApi(address);
            if (apiResult != null && !apiResult.contains("INVALID_USER_IP")) {
                return apiResult;
            }

            return getFallbackHospitalData(address);
        } catch (Exception e) {
            return getErrorResponse(address, e);
        }
    }

    private String callAmapApi(String address) {
        try {
            // 地理编码获取坐标
            URI geoUri = UriComponentsBuilder
                    .fromUriString("https://restapi.amap.com/v3/geocode/geo")
                    .queryParam("key", amapKey)
                    .queryParam("address", address)
                    .queryParam("output", "json")
                    .build()
                    .encode()
                    .toUri();

            ResponseEntity<String> geoResponse = restTemplate.exchange(geoUri, HttpMethod.GET,
                    new HttpEntity<>(createHeaders()), String.class);

            if (geoResponse.getStatusCode() != HttpStatus.OK || geoResponse.getBody() == null) {
                return null;
            }

            JsonNode geoData = objectMapper.readTree(geoResponse.getBody());
            String status = geoData.path("status").asText();
            String info = geoData.path("info").asText();

            if ("10005".equals(geoData.path("infocode").asText()) || "INVALID_USER_IP".equals(info)) {
                return "IP_ERROR:" + info;
            }

            if (!"1".equals(status)) {
                return null;
            }

            JsonNode geocodes = geoData.path("geocodes");
            if (geocodes.isEmpty() || !geocodes.isArray()) {
                return null;
            }

            String location = geocodes.get(0).path("location").asText();
            if (location == null || location.trim().isEmpty()) {
                return null;
            }

            return searchHospitals(location, address);
        } catch (Exception e) {
            return null;
        }
    }

    private String searchHospitals(String location, String originalAddress) {
        try {
            URI hospitalUri = UriComponentsBuilder
                    .fromUriString("https://restapi.amap.com/v3/place/around")
                    .queryParam("key", amapKey)
                    .queryParam("location", location)
                    .queryParam("types", "090100")
                    .queryParam("radius", "5000")
                    .queryParam("offset", "10")
                    .queryParam("page", "1")
                    .queryParam("extensions", "base")
                    .queryParam("output", "json")
                    .build()
                    .toUri();

            ResponseEntity<String> response = restTemplate.exchange(hospitalUri, HttpMethod.GET,
                    new HttpEntity<>(createHeaders()), String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return "❌ 医院搜索服务暂时不可用";
            }

            JsonNode data = objectMapper.readTree(response.getBody());
            if (!"1".equals(data.path("status").asText())) {
                return "⚠️ 医院查询失败: " + data.path("info").asText();
            }

            JsonNode pois = data.path("pois");
            if (pois.isEmpty()) {
                return "📍 在 " + originalAddress + " 附近 5 公里内未找到医院。";
            }

            return parseHospitalResults(pois, originalAddress);
        } catch (Exception e) {
            return "❌ 搜索医院时发生错误";
        }
    }

    private String parseHospitalResults(JsonNode pois, String originalAddress) {
        List<String> results = new ArrayList<>();

        for (JsonNode poi : pois) {
            String name = poi.path("name").asText();
            if (shouldFilterOut(name)) continue;

            StringBuilder result = new StringBuilder();
            result.append("🏥 ").append(name);

            String addressDetail = poi.path("address").asText();
            if (!addressDetail.trim().isEmpty()) {
                result.append("\n📍 地址：").append(addressDetail);
            }

            String tel = formatTel(poi.path("tel").asText());
            if (!tel.isEmpty()) {
                result.append("\n📞 电话：").append(tel);
            }

            String distance = poi.path("distance").asText();
            if (!distance.trim().isEmpty()) {
                result.append("\n📏 距离：").append(distance).append("米");
            }

            results.add(result.toString());
        }

        if (results.isEmpty()) {
            return "📍 在 " + originalAddress + " 附近找到了医疗机构，但暂无有效联系电话。";
        }

        StringBuilder output = new StringBuilder();
        output.append("🏥 在「").append(originalAddress).append("」附近找到以下医院：\n\n");

        int displayCount = Math.min(results.size(), 5);
        for (int i = 0; i < displayCount; i++) {
            output.append(i + 1).append(". ").append(results.get(i));
            if (i < displayCount - 1) output.append("\n\n");
        }

        if (results.size() > 5) {
            output.append("\n\n📋 共找到 ").append(results.size()).append(" 家医院，以上显示最近的5家");
        }

        output.append("\n\n💡 温馨提示：紧急情况请拨打120");

        return output.toString();
    }

    private String getFallbackHospitalData(String address) {
        String apiResult = callAmapApi(address);
        if (apiResult != null && apiResult.startsWith("IP_ERROR:")) {
            return getIpWhitelistErrorResponse(address);
        }
        return generateSimulatedHospitalData(address);
    }

    private String getIpWhitelistErrorResponse(String address) {
        String city = extractCity(address);

        return "🏥 在「" + address + "」附近医院信息（模拟数据）：\n\n" +
                generateHospitalDataByCity(city) +
                "\n\n⚠️ **重要提示：**\n" +
                "检测到高德地图API IP白名单限制。\n\n" +
                "**解决方案：**\n" +
                "1. 登录高德控制台添加IP白名单\n" +
                "2. 使用手机地图App搜索\"医院\"\n" +
                "3. 拨打120获取急救指导";
    }

    private String generateSimulatedHospitalData(String address) {
        String city = extractCity(address);

        return "🏥 在「" + address + "」附近医院信息（模拟数据）：\n\n" +
                generateHospitalDataByCity(city) +
                "\n\n💡 **温馨提示：**\n" +
                "• 以上为示例数据，请查询核实\n" +
                "• 紧急情况请拨打120\n" +
                "• 使用手机地图App获取实时信息";
    }

    private String generateHospitalDataByCity(String city) {
        if (city.contains("杭州") || city.contains("浙江")) {
            return "1. 浙江大学医学院附属第一医院\n" +
                    "📍 地址：杭州市上城区庆春路79号\n" +
                    "📞 电话：0571-87236114\n\n" +
                    "2. 浙江省人民医院\n" +
                    "📍 地址：杭州市下城区上塘路158号\n" +
                    "📞 电话：0571-85893111";
        } else if (city.contains("上海")) {
            return "1. 复旦大学附属华山医院\n" +
                    "📍 地址：上海市静安区乌鲁木齐中路12号\n" +
                    "📞 电话：021-52889999\n\n" +
                    "2. 上海交通大学医学院附属瑞金医院\n" +
                    "📍 地址：上海市黄浦区瑞金二路197号\n" +
                    "📞 电话：021-64370045";
        } else {
            return "1. 第一人民医院\n" +
                    "📍 地址：" + city + "市中心\n" +
                    "📞 电话：请查询当地114\n\n" +
                    "2. 中心医院\n" +
                    "📍 地址：" + city + "主要城区\n" +
                    "📞 电话：请咨询当地卫生部门";
        }
    }

    private String getErrorResponse(String address, Exception e) {
        return "❌ 医院查询服务暂时遇到问题。\n\n" +
                "**解决方案：**\n" +
                "1. 直接拨打120急救电话\n" +
                "2. 使用手机地图App搜索\"医院\"\n" +
                "3. 联系社区卫生服务中心";
    }

    private String extractCity(String address) {
        String[] cities = {"北京", "上海", "杭州", "广州", "深圳", "成都", "重庆", "武汉", "南京", "西安"};
        for (String city : cities) {
            if (address.contains(city)) return city;
        }
        return address.length() > 6 ? address.substring(0, 6) : address;
    }

    private boolean shouldFilterOut(String name) {
        if (name == null) return false;
        String lowerName = name.toLowerCase();
        return lowerName.contains("卫生室") || lowerName.contains("诊所") ||
                lowerName.contains("药店") || lowerName.contains("药房");
    }

    private String formatTel(String tel) {
        if (tel == null || tel.trim().isEmpty() || "[]".equals(tel) || "无".equals(tel)) {
            return "";
        }
        return tel.trim();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        headers.set("Accept", "application/json");
        return headers;
    }
}