// src/main/java/com/example/simpleagent/service/UserService.java
package com.example.simpleagent.service;

import com.example.simpleagent.model.User;
import com.example.simpleagent.model.HealthRecord;
import com.example.simpleagent.model.MedicationReminder;
import com.example.simpleagent.repository.UserRepository;
import com.example.simpleagent.repository.HealthRecordRepository;
import com.example.simpleagent.repository.MedicationReminderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private MedicationReminderRepository medicationReminderRepository;

    @Transactional
    public User register(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 设置默认值
        if (user.getRealName() == null) {
            user.setRealName(user.getUsername());
        }
        if (user.getAge() == null) {
            user.setAge(65); // 默认年龄65岁
        }
        if (user.getGender() == null) {
            user.setGender("其他"); // 默认性别
        }

        return userRepository.save(user);
    }

    public Optional<User> login(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            userRepository.save(user);
            return Optional.of(user);
        } else {
            // 用户不存在，自动创建
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setRealName(username);
            newUser.setAge(65);
            newUser.setGender("其他");
            newUser.setLastLoginTime(LocalDateTime.now());

            User savedUser = userRepository.save(newUser);
            return Optional.of(savedUser);
        }
    }

    public User updateProfile(Long userId, User updatedUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 更新基本信息（不能更改用户名）
        if (updatedUser.getRealName() != null) {
            user.setRealName(updatedUser.getRealName());
        }
        if (updatedUser.getAge() != null) {
            user.setAge(updatedUser.getAge());
        }
        if (updatedUser.getGender() != null) {
            user.setGender(updatedUser.getGender());
        }
        if (updatedUser.getPhone() != null) {
            user.setPhone(updatedUser.getPhone());
        }
        if (updatedUser.getEmergencyContact() != null) {
            user.setEmergencyContact(updatedUser.getEmergencyContact());
        }
        if (updatedUser.getEmergencyPhone() != null) {
            user.setEmergencyPhone(updatedUser.getEmergencyPhone());
        }
        if (updatedUser.getAddress() != null) {
            user.setAddress(updatedUser.getAddress());
        }
        if (updatedUser.getBloodType() != null) {
            user.setBloodType(updatedUser.getBloodType());
        }
        if (updatedUser.getChronicDiseases() != null) {
            user.setChronicDiseases(updatedUser.getChronicDiseases());
        }
        if (updatedUser.getAllergies() != null) {
            user.setAllergies(updatedUser.getAllergies());
        }
        if (updatedUser.getMedications() != null) {
            user.setMedications(updatedUser.getMedications());
        }
        if (updatedUser.getDoctorName() != null) {
            user.setDoctorName(updatedUser.getDoctorName());
        }
        if (updatedUser.getDoctorPhone() != null) {
            user.setDoctorPhone(updatedUser.getDoctorPhone());
        }

        return userRepository.save(user);
    }

    @Transactional
    public HealthRecord addHealthRecord(Long userId, HealthRecord record) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        record.setUser(user);
        return healthRecordRepository.save(record);
    }

    public List<HealthRecord> getHealthRecords(Long userId, String type) {
        if (type != null && !type.isEmpty()) {
            return healthRecordRepository.findByUserIdAndRecordType(userId, type);
        }
        return healthRecordRepository.findByUserIdOrderByRecordDateDesc(userId);
    }

    @Transactional
    public MedicationReminder addMedicationReminder(Long userId, MedicationReminder reminder) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        reminder.setUser(user);
        return medicationReminderRepository.save(reminder);
    }

    public List<MedicationReminder> getActiveMedicationReminders(Long userId) {
        return medicationReminderRepository.findActiveReminders(userId, java.time.LocalDate.now());
    }

    public Map<String, Object> getHealthSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        List<HealthRecord> recentRecords = healthRecordRepository
                .findByUserIdOrderByRecordDateDesc(userId);

        List<MedicationReminder> activeReminders = getActiveMedicationReminders(userId);

        Map<String, Object> summary = new HashMap<>();
        summary.put("user", user);
        summary.put("recentRecords", recentRecords);
        summary.put("activeReminders", activeReminders);

        // 计算统计信息
        Map<String, Object> stats = new HashMap<>();
        if (!recentRecords.isEmpty()) {
            stats.put("latestRecordDate", recentRecords.get(0).getRecordDate());
            stats.put("totalRecords", recentRecords.size());

            // 按类型统计
            Map<String, Integer> typeCount = new HashMap<>();
            for (HealthRecord record : recentRecords) {
                typeCount.put(record.getRecordType(),
                        typeCount.getOrDefault(record.getRecordType(), 0) + 1);
            }
            stats.put("recordTypeDistribution", typeCount);
        }

        summary.put("stats", stats);
        return summary;
    }
}