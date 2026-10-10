-- 独立电子借条主表，双方通过身份证号HMAC-SHA256值逻辑关联iou_user。
-- HMAC规则与密钥必须与用户表一致；不设外键，允许对方尚未注册。
-- 服务费由借款人支付，支付状态保存在独立服务费订单表。
-- 在eladmin数据库执行；重复执行不会覆盖已有表或数据。
USE `eladmin`;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `iou_note` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '借条ID',
  `note_no` varchar(32) NOT NULL COMMENT '借条编号',
  `creator_user_id` bigint NOT NULL COMMENT '发起人用户ID，逻辑关联iou_user.id',
  `borrower_name` varchar(64) NOT NULL COMMENT '借款人姓名快照',
  `borrower_id_no_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '借款人身份证HMAC-SHA256值，逻辑关联iou_user.id_no_hash',
  `borrower_id_no_encrypted` varchar(512) NOT NULL COMMENT '借款人身份证号密文快照',
  `lender_name` varchar(64) NOT NULL COMMENT '出借人姓名快照',
  `lender_id_no_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '出借人身份证HMAC-SHA256值，逻辑关联iou_user.id_no_hash',
  `lender_id_no_encrypted` varchar(512) NOT NULL COMMENT '出借人身份证号密文快照',
  `loan_type` tinyint NOT NULL DEFAULT 1 COMMENT '借款类型：1单次借款，2分期借款',
  `principal_amount` decimal(18,2) NOT NULL COMMENT '借款本金',
  `annual_rate` decimal(10,6) NOT NULL DEFAULT 0 COMMENT '年利率，0.15表示15%',
  `loan_date` date NOT NULL COMMENT '借款日期',
  `due_date` date NOT NULL COMMENT '最终还款日期',
  `interest_amount` decimal(18,2) NOT NULL COMMENT '总利息',
  `total_amount` decimal(18,2) NOT NULL COMMENT '应还本金加利息',
  `purpose` varchar(255) DEFAULT NULL COMMENT '借款用途',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `service_fee` decimal(18,2) NOT NULL DEFAULT 0 COMMENT '借款人应付服务费',
  `borrower_signed_at` datetime DEFAULT NULL COMMENT '借款人签署时间',
  `lender_signed_at` datetime DEFAULT NULL COMMENT '出借人签署时间',
  `agreement_file` varchar(255) DEFAULT NULL COMMENT '协议文件存储标识',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '借条状态：0草稿，1待对方确认，2已生效，3已结清，4已取消，5已拒绝，6待支付服务费',
  `effective_at` datetime DEFAULT NULL COMMENT '生效时间',
  `cancel_reason` varchar(255) DEFAULT NULL COMMENT '取消原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iou_note_note_no` (`note_no`),
  KEY `idx_iou_note_borrower_status` (`borrower_id_no_hash`, `status`),
  KEY `idx_iou_note_lender_status` (`lender_id_no_hash`, `status`),
  KEY `idx_iou_note_creator` (`creator_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子借条主表';
