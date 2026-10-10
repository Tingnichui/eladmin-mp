-- 电子借条新增状态6：待支付服务费，保留已有状态编号和数据。
-- 仅更新字段定义中的状态说明；业务状态转换由应用实现。
USE `eladmin`;
SET NAMES utf8mb4;

ALTER TABLE `iou_note`
  MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 0
  COMMENT '借条状态：0草稿，1待对方确认，2已生效，3已结清，4已取消，5已拒绝，6待支付服务费';
