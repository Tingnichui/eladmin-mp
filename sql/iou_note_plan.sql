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
