-- 将已核验借条账号迁移为确定性用户名和实名昵称。执行后必须清理变更账号的旧、新用户名缓存及会话。
-- 可重复执行，保留用户ID、密码、启停、角色、岗位与业务关联。仅处理已完成二要素核验的IOU账号。
USE `eladmin`;
SET NAMES utf8mb4;
START TRANSACTION;
SET @iou_username_guard = IF(
  EXISTS(SELECT 1 FROM iou_user i JOIN sys_user u ON u.user_id=i.sys_user_id
    WHERE u.user_type='IOU' AND i.id_verify_status=2
      AND (i.id_no_hash IS NULL OR i.id_no_hash NOT REGEXP '^[0-9a-f]{64}$' OR i.real_name IS NULL OR CHAR_LENGTH(TRIM(i.real_name)) NOT BETWEEN 1 AND 64))
  OR EXISTS(SELECT 1 FROM iou_user i JOIN sys_user u ON u.user_id=i.sys_user_id
    JOIN sys_user other ON other.username=CONCAT('iou_',i.id_no_hash) AND other.user_id<>u.user_id
    WHERE u.user_type='IOU' AND i.id_verify_status=2),
  'SELECT * FROM iou_username_migration_requires_manual_review', 'DO 0');
PREPARE iou_username_check FROM @iou_username_guard;
EXECUTE iou_username_check;
DEALLOCATE PREPARE iou_username_check;
UPDATE sys_user u JOIN iou_user i ON i.sys_user_id=u.user_id
SET u.username=CONCAT('iou_',i.id_no_hash), u.nick_name=TRIM(i.real_name), u.update_by='System', u.update_time=NOW()
WHERE u.user_type='IOU' AND i.id_verify_status=2
  AND (BINARY u.username<>BINARY CONCAT('iou_',i.id_no_hash) OR NOT (BINARY u.nick_name <=> BINARY TRIM(i.real_name)));
COMMIT;
