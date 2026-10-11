-- 支持先签署后开放链接，一张主表加一张还款计划表。
-- 只扩展结构和字典，不伪造旧数据的计划、角色或签署记录。MySQL 5.7+/8.x。
USE `eladmin`;
SET NAMES utf8mb4;
ALTER TABLE `iou_note` MODIFY COLUMN `borrower_name` varchar(64) DEFAULT NULL COMMENT '借款人姓名快照，对方签署前可为空';
ALTER TABLE `iou_note` MODIFY COLUMN `borrower_id_no_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '借款人身份证HMAC-SHA256值，逻辑关联iou_user.id_no_hash';
ALTER TABLE `iou_note` MODIFY COLUMN `borrower_id_no_encrypted` varchar(512) DEFAULT NULL COMMENT '借款人身份证号密文快照';
ALTER TABLE `iou_note` MODIFY COLUMN `lender_name` varchar(64) DEFAULT NULL COMMENT '出借人姓名快照，对方签署前可为空';
ALTER TABLE `iou_note` MODIFY COLUMN `lender_id_no_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '出借人身份证HMAC-SHA256值，逻辑关联iou_user.id_no_hash';
ALTER TABLE `iou_note` MODIFY COLUMN `lender_id_no_encrypted` varchar(512) DEFAULT NULL COMMENT '出借人身份证号密文快照';
ALTER TABLE `iou_note` MODIFY COLUMN `annual_rate` decimal(12,8) NOT NULL DEFAULT 0 COMMENT '年利率，0.15表示15%';
ALTER TABLE `iou_note` MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 0 COMMENT '借条状态：0草稿，1待对方确认，2已生效，3已结清，4已取消，5已拒绝，6待支付服务费，7待对方签署';

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='creator_role'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `creator_role` tinyint DEFAULT NULL COMMENT ''发起人身份：1借款人，2出借人，旧记录可为空''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='repayment_method'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `repayment_method` varchar(16) DEFAULT NULL COMMENT ''once/annuity/principal/interest/flat/custom''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='installment_count'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `installment_count` int DEFAULT NULL COMMENT ''约定还款期数，一次性为1''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='repayment_cycle_months'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `repayment_cycle_months` int DEFAULT NULL COMMENT ''标准还款周期月数，自定义可为空''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='first_due_date'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `first_due_date` date DEFAULT NULL COMMENT ''首期还款日期''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='interest_rule'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `interest_rule` varchar(32) DEFAULT NULL COMMENT ''计息规则标识''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='calculation_version'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `calculation_version` varchar(32) DEFAULT NULL COMMENT ''后端计算规则版本''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='prepayment_rule'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `prepayment_rule` varchar(32) DEFAULT NULL COMMENT ''提前还款规则标识''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='overdue_terms'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `overdue_terms` varchar(1000) DEFAULT NULL COMMENT ''逾期约定正文''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='terms_snapshot'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `terms_snapshot` longtext COMMENT ''发起人签署的条款与完整还款计划快照，内部使用''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='terms_hash'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `terms_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT ''规范化条款快照SHA256''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='terms_locked_at'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `terms_locked_at` datetime DEFAULT NULL COMMENT ''开始签署时锁定条款时间''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='borrower_sign_status'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `borrower_sign_status` tinyint NOT NULL DEFAULT 0 COMMENT ''借款人签署：0未发起，1处理中，2成功，3失败''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='borrower_sign_request_id'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `borrower_sign_request_id` varchar(128) DEFAULT NULL COMMENT ''借款人签署请求ID，内部使用''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='borrower_sign_evidence_id'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `borrower_sign_evidence_id` varchar(255) DEFAULT NULL COMMENT ''借款人签署凭证标识，内部使用''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='borrower_sign_file'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `borrower_sign_file` varchar(512) DEFAULT NULL COMMENT ''借款人签署文件存储标识，内部使用''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='borrower_sign_file_hash'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `borrower_sign_file_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT ''借款人签署文件SHA256''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='lender_sign_status'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `lender_sign_status` tinyint NOT NULL DEFAULT 0 COMMENT ''出借人签署：0未发起，1处理中，2成功，3失败''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='lender_sign_request_id'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `lender_sign_request_id` varchar(128) DEFAULT NULL COMMENT ''出借人签署请求ID，内部使用''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='lender_sign_evidence_id'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `lender_sign_evidence_id` varchar(255) DEFAULT NULL COMMENT ''出借人签署凭证标识，内部使用''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='lender_sign_file'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `lender_sign_file` varchar(512) DEFAULT NULL COMMENT ''出借人签署文件存储标识，内部使用''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='lender_sign_file_hash'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `lender_sign_file_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT ''出借人签署文件SHA256''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='agreement_hash'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `agreement_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT ''最终协议文件SHA256''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

SET @iou_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='iou_note' AND column_name='row_version'), 'SELECT 1', 'ALTER TABLE `iou_note` ADD COLUMN `row_version` bigint NOT NULL DEFAULT 0 COMMENT ''并发版本号，由条件更新推进''');
PREPARE iou_stmt FROM @iou_ddl;
EXECUTE iou_stmt;
DEALLOCATE PREPARE iou_stmt;

-- 只保存约定还款计划，不保存实际还款流水；一次性还款也保存一期。
USE `eladmin`;
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS `iou_note_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '还款计划ID',
  `note_id` bigint NOT NULL COMMENT '所属iou_note.id',
  `period_no` int NOT NULL COMMENT '从1开始的期次',
  `due_date` date NOT NULL COMMENT '本期约定还款日期',
  `principal_amount` decimal(18,2) NOT NULL COMMENT '本期应还本金',
  `interest_amount` decimal(18,2) NOT NULL COMMENT '本期应还利息',
  `total_amount` decimal(18,2) NOT NULL COMMENT '本期应还本息合计',
  `remaining_principal` decimal(18,2) NOT NULL COMMENT '按计划还完本期后的剩余本金，非实际余额',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iou_note_plan_period` (`note_id`, `period_no`),
  CONSTRAINT `fk_iou_note_plan_note` FOREIGN KEY (`note_id`) REFERENCES `iou_note` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子借条约定还款计划';

-- 电子借条模块字典，在 eladmin 主库执行，可重复执行。
USE `eladmin`;
SET NAMES utf8mb4;
START TRANSACTION;
INSERT INTO sys_dict (name,description,create_by,update_by,create_time,update_time) SELECT 'iou_user_verify_status','电子借条用户认证状态，iou_user.verify_status','admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE name='iou_user_verify_status');
SET @iou_dict_id := (SELECT dict_id FROM sys_dict WHERE name='iou_user_verify_status' ORDER BY dict_id LIMIT 1);
INSERT INTO sys_dict_detail (dict_id,label,value,dict_sort,create_by,update_by,create_time,update_time) SELECT @iou_dict_id,'未认证','0',10,'admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_detail WHERE dict_id=@iou_dict_id AND value='0');
INSERT INTO sys_dict_detail (dict_id,label,value,dict_sort,create_by,update_by,create_time,update_time) SELECT @iou_dict_id,'认证中','1',20,'admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_detail WHERE dict_id=@iou_dict_id AND value='1');
INSERT INTO sys_dict_detail (dict_id,label,value,dict_sort,create_by,update_by,create_time,update_time) SELECT @iou_dict_id,'已认证','2',30,'admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_detail WHERE dict_id=@iou_dict_id AND value='2');
INSERT INTO sys_dict_detail (dict_id,label,value,dict_sort,create_by,update_by,create_time,update_time) SELECT @iou_dict_id,'认证失败','3',40,'admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_detail WHERE dict_id=@iou_dict_id AND value='3');
INSERT INTO sys_dict (name,description,create_by,update_by,create_time,update_time) SELECT 'iou_note_loan_type','电子借条借款类型，iou_note.loan_type','admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE name='iou_note_loan_type');
SET @iou_dict_id := (SELECT dict_id FROM sys_dict WHERE name='iou_note_loan_type' ORDER BY dict_id LIMIT 1);
INSERT INTO sys_dict_detail (dict_id,label,value,dict_sort,create_by,update_by,create_time,update_time) SELECT @iou_dict_id,'单次借款','1',10,'admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_detail WHERE dict_id=@iou_dict_id AND value='1');
INSERT INTO sys_dict_detail (dict_id,label,value,dict_sort,create_by,update_by,create_time,update_time) SELECT @iou_dict_id,'分期借款','2',20,'admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_detail WHERE dict_id=@iou_dict_id AND value='2');
INSERT INTO sys_dict (name,description,create_by,update_by,create_time,update_time) SELECT 'iou_note_status','电子借条状态，iou_note.status','admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE name='iou_note_status');
SET @iou_dict_id := (SELECT dict_id FROM sys_dict WHERE name='iou_note_status' ORDER BY dict_id LIMIT 1);
INSERT INTO sys_dict_detail (dict_id,label,value,dict_sort,create_by,update_by,create_time,update_time) SELECT @iou_dict_id,'草稿','0',10,'admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_detail WHERE dict_id=@iou_dict_id AND value='0');
INSERT INTO sys_dict_detail (dict_id,label,value,dict_sort,create_by,update_by,create_time,update_time) SELECT @iou_dict_id,'待对方确认','1',20,'admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_detail WHERE dict_id=@iou_dict_id AND value='1');
INSERT INTO sys_dict_detail (dict_id,label,value,dict_sort,create_by,update_by,create_time,update_time) SELECT @iou_dict_id,'已生效','2',30,'admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_detail WHERE dict_id=@iou_dict_id AND value='2');
INSERT INTO sys_dict_detail (dict_id,label,value,dict_sort,create_by,update_by,create_time,update_time) SELECT @iou_dict_id,'已结清','3',40,'admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_detail WHERE dict_id=@iou_dict_id AND value='3');
INSERT INTO sys_dict_detail (dict_id,label,value,dict_sort,create_by,update_by,create_time,update_time) SELECT @iou_dict_id,'已取消','4',50,'admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_detail WHERE dict_id=@iou_dict_id AND value='4');
INSERT INTO sys_dict_detail (dict_id,label,value,dict_sort,create_by,update_by,create_time,update_time) SELECT @iou_dict_id,'已拒绝','5',60,'admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_detail WHERE dict_id=@iou_dict_id AND value='5');
INSERT INTO sys_dict_detail (dict_id,label,value,dict_sort,create_by,update_by,create_time,update_time) SELECT @iou_dict_id,'待支付服务费','6',70,'admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_detail WHERE dict_id=@iou_dict_id AND value='6');
INSERT INTO sys_dict_detail (dict_id,label,value,dict_sort,create_by,update_by,create_time,update_time) SELECT @iou_dict_id,'待对方签署','7',80,'admin','admin',NOW(),NOW() WHERE NOT EXISTS (SELECT 1 FROM sys_dict_detail WHERE dict_id=@iou_dict_id AND value='7');
COMMIT;
