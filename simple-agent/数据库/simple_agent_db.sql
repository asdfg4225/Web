/*
 Navicat Premium Data Transfer

 Source Server         : 202305315223
 Source Server Type    : MySQL
 Source Server Version : 80044
 Source Host           : localhost:3306
 Source Schema         : simple_agent_db

 Target Server Type    : MySQL
 Target Server Version : 80044
 File Encoding         : 65001

 Date: 30/12/2025 14:36:41
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for health_records
-- ----------------------------
DROP TABLE IF EXISTS `health_records`;
CREATE TABLE `health_records`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `record_date` date NOT NULL,
  `record_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `record_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `unit` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `recorded_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_date`(`user_id`, `record_date`) USING BTREE,
  CONSTRAINT `health_records_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of health_records
-- ----------------------------
INSERT INTO `health_records` VALUES (1, 1, '2024-01-15', '血压', '130/85', 'mmHg', NULL, '用户本人', '2025-12-30 11:32:18', '2025-12-30 11:32:18');
INSERT INTO `health_records` VALUES (2, 1, '2024-01-15', '血糖', '6.5', 'mmol/L', NULL, '用户本人', '2025-12-30 11:32:18', '2025-12-30 11:32:18');
INSERT INTO `health_records` VALUES (3, 2, '2024-01-15', '血压', '145/90', 'mmHg', NULL, '用户本人', '2025-12-30 11:32:18', '2025-12-30 11:32:18');
INSERT INTO `health_records` VALUES (4, 3, '2024-01-15', '血糖', '7.2', 'mmol/L', NULL, '用户本人', '2025-12-30 11:32:18', '2025-12-30 11:32:18');
INSERT INTO `health_records` VALUES (6, 1, '2025-12-30', '体重', '75', 'kg', '', '用户本人', '2025-12-30 13:05:36', '2025-12-30 13:05:36');

-- ----------------------------
-- Table structure for medication_reminders
-- ----------------------------
DROP TABLE IF EXISTS `medication_reminders`;
CREATE TABLE `medication_reminders`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `medication_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `dosage` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `frequency` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `time_of_day` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `start_date` date NULL DEFAULT NULL,
  `end_date` date NULL DEFAULT NULL,
  `is_active` tinyint(1) NULL DEFAULT 1,
  `last_taken` datetime(0) NULL DEFAULT NULL,
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_active`(`user_id`, `is_active`) USING BTREE,
  CONSTRAINT `medication_reminders_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of medication_reminders
-- ----------------------------
INSERT INTO `medication_reminders` VALUES (1, 1, '降压药', '1片', '每日一次', '早上8点', '2024-01-01', NULL, 1, NULL, NULL, '2025-12-30 11:32:18', '2025-12-30 11:32:18');
INSERT INTO `medication_reminders` VALUES (2, 1, '降糖药', '0.5片', '每日两次', '早晚饭后', '2024-01-01', NULL, 1, NULL, NULL, '2025-12-30 11:32:18', '2025-12-30 11:32:18');
INSERT INTO `medication_reminders` VALUES (3, 2, '关节止痛药', '1片', '需要时服用', '疼痛时', '2024-01-01', NULL, 1, NULL, NULL, '2025-12-30 11:32:18', '2025-12-30 11:32:18');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `real_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `age` int(0) NULL DEFAULT NULL,
  `gender` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `emergency_contact` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `emergency_phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `blood_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `chronic_diseases` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `allergies` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `medications` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `doctor_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `doctor_phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `last_login_time` datetime(0) NULL DEFAULT NULL,
  `created_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'zhangsan', '张三', 75, '男', '13800138001', NULL, NULL, '北京市朝阳区XX街道XX号', 'A', '高血压,糖尿病', '青霉素过敏', NULL, NULL, NULL, '2025-12-30 14:29:32', '2025-12-30 11:32:18', '2025-12-30 14:29:32');
INSERT INTO `users` VALUES (2, 'lisi', '李四', 68, '女', '13900139001', NULL, NULL, '上海市浦东新区XX路XX号', 'O', '关节炎', '海鲜过敏', NULL, NULL, NULL, NULL, '2025-12-30 11:32:18', '2025-12-30 11:32:18');
INSERT INTO `users` VALUES (3, 'wangwu', '王五', 72, '男', '13700137001', NULL, NULL, '广州市天河区XX街道XX号', 'B', '高血压', '无', NULL, NULL, NULL, NULL, '2025-12-30 11:32:18', '2025-12-30 11:32:18');
INSERT INTO `users` VALUES (4, 'zhaoliu', '赵六', 80, '女', '13600136001', NULL, NULL, '深圳市南山区XX路XX号', 'AB', '糖尿病,冠心病', '花粉过敏', NULL, NULL, NULL, NULL, '2025-12-30 11:32:18', '2025-12-30 11:32:18');
INSERT INTO `users` VALUES (5, 'test', '测试用户', 65, '男', '13500135001', NULL, NULL, '杭州市西湖区XX街道XX号', 'O', '无', '无', NULL, NULL, NULL, NULL, '2025-12-30 11:32:18', '2025-12-30 11:32:18');

SET FOREIGN_KEY_CHECKS = 1;
