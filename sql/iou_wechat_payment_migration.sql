-- 微信公众号直连服务费；先执行 iou_note_business_migration.sql。
-- 仅新增主表两个字段与索引；不创建支付表，不给历史借条补收费。
USE `eladmin`;
SET NAMES utf8mb4;
SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='payment_order_no'), 'SELECT 1', 'ALTER TABLE iou_note ADD COLUMN payment_order_no varchar(32) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT ''当前微信商户订单号''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;
SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='service_fee_pay_status'), 'SELECT 1', 'ALTER TABLE iou_note ADD COLUMN service_fee_pay_status tinyint NOT NULL DEFAULT 0 COMMENT ''服务费支付状态0未付1待确认2已付3已关闭''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;
SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='iou_note' AND index_name='uk_iou_note_payment_order'), 'SELECT 1', 'ALTER TABLE iou_note ADD UNIQUE KEY uk_iou_note_payment_order(payment_order_no)');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;
SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='iou_note' AND index_name='idx_iou_note_fee_pending'), 'SELECT 1', 'ALTER TABLE iou_note ADD KEY idx_iou_note_fee_pending(service_fee_pay_status, update_time, id)');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;
