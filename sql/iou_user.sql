-- 独立电子借条模块用户表：手机号或身份证号密码登录，三要素核验。
-- 在 eladmin 数据库执行；重复执行不会覆盖已有表或数据。
USE `eladmin`;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `iou_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `mobile` varchar(11) NOT NULL COMMENT '登录手机号',
  `password_hash` varchar(255) NOT NULL COMMENT '登录密码哈希（BCrypt），禁止存储明文',
  `real_name` varchar(64) DEFAULT NULL COMMENT '真实姓名',
  `id_no_encrypted` varchar(512) DEFAULT NULL COMMENT '身份证号密文',
  `id_no_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '身份证号HMAC-SHA256值，用于认证通过后的登录查询和唯一绑定',
  `verify_status` tinyint NOT NULL DEFAULT 0 COMMENT '三要素认证状态：0未认证，1认证中，2已认证，3认证失败',
  `verified_at` datetime DEFAULT NULL COMMENT '认证通过时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iou_user_mobile` (`mobile`),
  UNIQUE KEY `uk_iou_user_id_no_hash` (`id_no_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子借条独立用户';
