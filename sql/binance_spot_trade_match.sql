-- 现货 FIFO 撮合结果与成交撮合状态。
-- 第一阶段沿用现有系统约束，trade_id 按全局唯一处理。

CREATE TABLE IF NOT EXISTS `binance_spot_trade_match` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `uid` int NOT NULL COMMENT '币安账户用户编号',
  `symbol` varchar(32) NOT NULL COMMENT '现货交易对',
  `buy_trade_id` bigint NOT NULL COMMENT '买入成交 ID',
  `sell_trade_id` bigint NOT NULL COMMENT '卖出成交 ID',
  `matched_qty` decimal(32,16) NOT NULL COMMENT '撮合数量',
  `buy_price` decimal(32,16) NOT NULL COMMENT '买入价格快照',
  `sell_price` decimal(32,16) NOT NULL COMMENT '卖出价格快照',
  `buy_time` datetime(3) NOT NULL COMMENT '买入成交时间',
  `sell_time` datetime(3) NOT NULL COMMENT '卖出成交时间',
  `buy_amount` decimal(32,16) NOT NULL COMMENT '撮合买入金额',
  `sell_amount` decimal(32,16) NOT NULL COMMENT '撮合卖出金额',
  `fee_rate` decimal(16,10) NOT NULL DEFAULT '0.0010000000' COMMENT '手续费率',
  `pnl` decimal(32,16) NOT NULL COMMENT '已实现盈亏',
  `fee` decimal(32,16) NOT NULL COMMENT '手续费',
  `net_pnl` decimal(32,16) NOT NULL COMMENT '扣除手续费后的净盈亏',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_spot_match_buy_trade` (`buy_trade_id`),
  KEY `idx_spot_match_sell_trade` (`sell_trade_id`),
  KEY `idx_spot_match_stats` (`uid`,`symbol`,`sell_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='币安现货 FIFO 撮合明细';

CREATE TABLE IF NOT EXISTS `binance_spot_trade_match_state` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `trade_id` bigint NOT NULL COMMENT '币安成交 ID',
  `uid` int NOT NULL COMMENT '币安账户用户编号',
  `symbol` varchar(32) NOT NULL COMMENT '现货交易对',
  `is_buyer` tinyint NOT NULL COMMENT '是否为买方：1买入，0卖出',
  `trade_time` datetime(3) NOT NULL COMMENT '成交时间',
  `original_qty` decimal(32,16) NOT NULL COMMENT '原始成交数量',
  `matched_qty` decimal(32,16) NOT NULL DEFAULT '0.0000000000000000' COMMENT '已撮合数量',
  `remaining_qty` decimal(32,16) NOT NULL COMMENT '剩余未撮合数量',
  `match_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '撮合状态：PENDING、PARTIAL、COMPLETED、EXCEPTION',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_spot_match_state_trade` (`trade_id`),
  KEY `idx_spot_match_state_fifo` (`uid`,`symbol`,`is_buyer`,`match_status`,`trade_time`,`trade_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='币安现货成交撮合状态';
