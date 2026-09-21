-- 币安 C2C 历史订单。
-- 币安接口字段按 snake_case 保存；uid、order_time、sync_time 与审计字段为本地扩展字段。

CREATE TABLE IF NOT EXISTS `binance_c2c_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `uid` int NOT NULL COMMENT '币安账户用户编号',
  `order_number` varchar(64) NOT NULL COMMENT '币安订单号（orderNumber）',
  `adv_no` varchar(64) DEFAULT NULL COMMENT '广告编号（advNo）',
  `trade_type` varchar(16) NOT NULL COMMENT '交易方向（tradeType）：BUY、SELL',
  `asset` varchar(16) NOT NULL COMMENT '数字资产（asset），如 USDT',
  `fiat` varchar(16) NOT NULL COMMENT '法币（fiat），如 CNY',
  `fiat_symbol` varchar(16) DEFAULT NULL COMMENT '法币符号（fiatSymbol）',
  `amount` decimal(32,16) DEFAULT NULL COMMENT '数字资产数量（amount）',
  `taker_amount` decimal(32,16) DEFAULT NULL COMMENT '数字资产数量（takerAmount）',
  `total_price` decimal(32,16) NOT NULL COMMENT '法币总金额（totalPrice）',
  `unit_price` decimal(32,16) DEFAULT NULL COMMENT '成交单价（unitPrice）',
  `order_status` varchar(32) NOT NULL COMMENT '订单状态（orderStatus）',
  `order_create_time` bigint NOT NULL COMMENT '币安订单创建时间戳（createTime，毫秒）',
  `order_time` datetime(3) NOT NULL COMMENT '币安订单创建时间（本地转换值）',
  `commission` decimal(32,16) DEFAULT NULL COMMENT '手续费（commission）',
  `counter_part_nick_name` varchar(255) DEFAULT NULL COMMENT '交易对手昵称（counterPartNickName）',
  `advertisement_role` varchar(32) DEFAULT NULL COMMENT '广告角色（advertisementRole）',
  `raw_data` longtext COMMENT '币安接口原始 JSON',
  `sync_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最近同步时间',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_c2c_order_account_number` (`uid`,`order_number`),
  KEY `idx_c2c_order_stats` (`uid`,`asset`,`fiat`,`trade_type`,`order_status`,`order_time`),
  KEY `idx_c2c_order_sync_time` (`sync_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='币安 C2C 历史订单';
