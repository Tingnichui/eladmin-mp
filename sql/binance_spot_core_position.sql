-- 币安现货底仓锁定记录
-- 用于将指定现货买入成交的部分或全部数量排除在普通 FIFO 平仓撮合之外。

CREATE TABLE IF NOT EXISTS `binance_spot_core_position` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `uid` int NOT NULL COMMENT '币安账户用户编号',
  `symbol` varchar(32) NOT NULL COMMENT '现货交易对，如 BTCUSDT',
  `trade_id` bigint NOT NULL COMMENT '原始现货买入成交 ID',
  `core_qty` decimal(32,16) NOT NULL COMMENT '锁定为底仓的数量',
  `locked_at` datetime(3) NOT NULL COMMENT '设为底仓时间',
  `released_at` datetime(3) DEFAULT NULL COMMENT '解除底仓时间，空表示仍在锁定',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(255) DEFAULT NULL COMMENT '更新者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_core_trade` (`trade_id`, `released_at`),
  KEY `idx_core_account_symbol` (`uid`, `symbol`, `released_at`),
  KEY `idx_core_replay` (`uid`, `symbol`, `locked_at`, `released_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='币安现货底仓锁定记录';
