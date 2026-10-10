-- 已有电子借条用户表增加身份证号登录查询字段，可重复执行。
-- HMAC-SHA256由应用使用独立密钥计算，数据库不保存密钥。
-- 认证通过后写入规范化身份证号的HMAC值；未认证用户保持NULL。
USE `eladmin`;
SET NAMES utf8mb4;

SET @iou_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'eladmin' AND table_name = 'iou_user' AND column_name = 'id_no_hash'),
  'SELECT 1',
  'ALTER TABLE `iou_user` ADD COLUMN `id_no_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT ''身份证号HMAC-SHA256值，用于认证通过后的登录查询和唯一绑定'' AFTER `id_no_encrypted`'
);
PREPARE iou_stmt FROM @iou_sql;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics
    WHERE table_schema = 'eladmin' AND table_name = 'iou_user' AND index_name = 'uk_iou_user_id_no_hash'),
  'SELECT 1',
  'ALTER TABLE `iou_user` ADD UNIQUE KEY `uk_iou_user_id_no_hash` (`id_no_hash`)'
);
PREPARE iou_stmt FROM @iou_sql;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;
