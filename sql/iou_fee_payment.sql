-- 斗拱旧设计保留，仅作为后续扩展参考。当前微信直连不执行本脚本。
-- 同一借条一张服务费订单；重试只新增已确认终态之后的支付尝试。
USE `eladmin`;
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS `iou_fee_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(34) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `note_id` bigint NOT NULL,
  `borrower_user_id` bigint NOT NULL,
  `amount` decimal(18,2) NOT NULL,
  `status` varchar(16) CHARACTER SET ascii NOT NULL,
  `fee_snapshot` longtext NOT NULL,
  `payer_openid` varchar(128) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `oauth_state_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `oauth_expires_at` datetime(3) DEFAULT NULL,
  `oauth_consumed_at` datetime(3) DEFAULT NULL,
  `paid_at` datetime(3) DEFAULT NULL,
  `create_time` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iou_fee_order_note` (`note_id`),
  UNIQUE KEY `uk_iou_fee_order_no` (`order_no`),
  UNIQUE KEY `uk_iou_fee_oauth_state` (`oauth_state_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借款方平台服务费订单';
CREATE TABLE IF NOT EXISTS `iou_payment_attempt` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fee_order_id` bigint NOT NULL,
  `req_seq_id` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `req_date` char(8) CHARACTER SET ascii NOT NULL,
  `huifu_id` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `hf_seq_id` varchar(128) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `status` varchar(16) CHARACTER SET ascii NOT NULL,
  `pay_info` text DEFAULT NULL,
  `expires_at` datetime(3) NOT NULL,
  `checked_at` datetime(3) DEFAULT NULL,
  `create_time` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iou_payment_req` (`req_seq_id`),
  KEY `idx_iou_payment_order` (`fee_order_id`,`id`),
  KEY `idx_iou_payment_pending` (`status`,`checked_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='汇付支付尝试与补偿查询';
