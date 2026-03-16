// src/main/java/com/example/simpleagent/controller/UserController.java
package com.example.simpleagent.controller;

import com.example.simpleagent.model.User;
import com.example.simpleagent.model.HealthRecord;
import com.example.simpleagent.model.MedicationReminder;
import com.example.simpleagent.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    // 注册（简化版，无需密码）
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User registeredUser = userService.register(user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "注册成功");
            response.put("user", registeredUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 登录（简化版，仅凭用户名）
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials,
                                   HttpSession session) {
        String username = credentials.get("username");

        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("用户名不能为空");
        }

        try {
            var userOptional = userService.login(username.trim());
            if (userOptional.isPresent()) {
                User user = userOptional.get();

                // 重要：正确设置session属性
                System.out.println("🔐 设置session: userId=" + user.getId() + ", username=" + user.getUsername());
                session.setAttribute("userId", user.getId());
                session.setAttribute("username", user.getUsername());

                // 设置session属性为Long类型，确保类型正确
                session.setAttribute("userId", user.getId()); // 确保这是Long类型

                // 确认session设置成功
                System.out.println("🔐 Session ID: " + session.getId());
                System.out.println("🔐 Session创建时间: " + new Date(session.getCreationTime()));

                // 设置session有效期（30分钟）
                session.setMaxInactiveInterval(30 * 60);

                // 获取健康摘要
                Map<String, Object> healthSummary = userService.getHealthSummary(user.getId());

                Map<String, Object> response = new HashMap<>();
                response.put("message", "登录成功");
                response.put("user", user);
                response.put("healthSummary", healthSummary);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("登录失败");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("登录失败：" + e.getMessage());
        }
    }

    // 登出
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("登出成功");
    }

    // 获取当前用户信息
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("请先登录");
        }

        try {
            Map<String, Object> healthSummary = userService.getHealthSummary(userId);
            return ResponseEntity.ok(healthSummary);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("获取信息失败：" + e.getMessage());
        }
    }

    // 更新用户信息
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody User updatedUser,
                                           HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("请先登录");
        }

        try {
            User user = userService.updateProfile(userId, updatedUser);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 添加健康记录
    @PostMapping("/health-records")
    public ResponseEntity<?> addHealthRecord(@RequestBody HealthRecord record,
                                             HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("请先登录");
        }

        try {
            HealthRecord savedRecord = userService.addHealthRecord(userId, record);
            return ResponseEntity.ok(savedRecord);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 获取健康记录
    @GetMapping("/health-records")
    public ResponseEntity<?> getHealthRecords(
            @RequestParam(required = false) String type,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("请先登录");
        }

        List<HealthRecord> records = userService.getHealthRecords(userId, type);
        return ResponseEntity.ok(records);
    }

    // 添加用药提醒
    @PostMapping("/medication-reminders")
    public ResponseEntity<?> addMedicationReminder(@RequestBody MedicationReminder reminder,
                                                   HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("请先登录");
        }

        try {
            MedicationReminder savedReminder = userService.addMedicationReminder(userId, reminder);
            return ResponseEntity.ok(savedReminder);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 获取用药提醒
    @GetMapping("/medication-reminders")
    public ResponseEntity<?> getMedicationReminders(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("请先登录");
        }

        List<MedicationReminder> reminders = userService.getActiveMedicationReminders(userId);
        return ResponseEntity.ok(reminders);
    }

    // 标记用药已服用
    @PostMapping("/medication-reminders/{id}/take")
    public ResponseEntity<?> markAsTaken(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("请先登录");
        }

        try {
            // 这里需要实现标记用药为已服用的逻辑
            // 暂时返回成功消息
            return ResponseEntity.ok("标记成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("标记失败：" + e.getMessage());
        }
    }

    // 快速登录示例用户
    @PostMapping("/quick-login/{username}")
    public ResponseEntity<?> quickLogin(@PathVariable String username,
                                        HttpSession session) {
        try {
            var userOptional = userService.login(username);
            if (userOptional.isPresent()) {
                User user = userOptional.get();

                // 正确设置session
                System.out.println("🔐 快速登录设置session: userId=" + user.getId() + ", username=" + user.getUsername());
                session.setAttribute("userId", user.getId());
                session.setAttribute("username", user.getUsername());

                // 设置session属性为Long类型
                session.setAttribute("userId", user.getId());

                // 确认session设置
                System.out.println("🔐 快速登录Session ID: " + session.getId());

                // 设置session有效期
                session.setMaxInactiveInterval(30 * 60);

                // 获取健康摘要
                Map<String, Object> healthSummary = userService.getHealthSummary(user.getId());

                Map<String, Object> response = new HashMap<>();
                response.put("message", "快速登录成功");
                response.put("user", user);
                response.put("healthSummary", healthSummary);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("快速登录失败");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("快速登录失败：" + e.getMessage());
        }
    }
}