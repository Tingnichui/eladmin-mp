-- UTF-8. Run before deploying the new number generator; existing notes are untouched.
CREATE TABLE IF NOT EXISTS `iou_note_number` (
  `number_date` date NOT NULL COMMENT '北京时间日期',
  `last_value` bigint NOT NULL DEFAULT 0 COMMENT '最后分配的时间编码，不代表借条数量',
  PRIMARY KEY (`number_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借条编号每日分配记录';
