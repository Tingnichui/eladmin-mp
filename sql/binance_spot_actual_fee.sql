-- 现货实际手续费及折算金额。
-- 执行应用升级前请先备份 binance_trade_info、binance_spot_trade_match、
-- binance_spot_trade_match_state 和 binance_spot_core_position。

ALTER TABLE `binance_trade_info`
  ADD COLUMN `commission_quote_asset` varchar(16) DEFAULT NULL COMMENT '手续费折算计价资产' AFTER `commission_asset`,
  ADD COLUMN `commission_quote_price` decimal(32,16) DEFAULT NULL COMMENT '手续费资产折算价格' AFTER `commission_quote_asset`,
  ADD COLUMN `commission_quote_amount` decimal(32,16) DEFAULT NULL COMMENT '折算后的手续费' AFTER `commission_quote_price`,
  ADD COLUMN `commission_valuation_source` varchar(20) DEFAULT NULL COMMENT '手续费估值来源' AFTER `commission_quote_amount`,
  ADD COLUMN `commission_valuation_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '手续费估值状态' AFTER `commission_valuation_source`,
  ADD KEY `idx_trade_commission_valuation` (`uid`, `symbol`, `commission_valuation_status`);

ALTER TABLE `binance_spot_trade_match`
  MODIFY COLUMN `fee_rate` decimal(16,10) NOT NULL DEFAULT '0.0000000000' COMMENT '兼容字段，新撮合不再按固定费率计算',
  MODIFY COLUMN `net_pnl` decimal(32,16) DEFAULT NULL COMMENT '扣除手续费后的净盈亏；手续费待估值时为空',
  ADD COLUMN `buy_fee_asset` varchar(16) DEFAULT NULL COMMENT '买入手续费资产' AFTER `fee_rate`,
  ADD COLUMN `buy_fee_amount` decimal(32,16) DEFAULT NULL COMMENT '分摊的买入原币手续费' AFTER `buy_fee_asset`,
  ADD COLUMN `buy_fee_quote_amount` decimal(32,16) DEFAULT NULL COMMENT '分摊的买入折算手续费' AFTER `buy_fee_amount`,
  ADD COLUMN `sell_fee_asset` varchar(16) DEFAULT NULL COMMENT '卖出手续费资产' AFTER `buy_fee_quote_amount`,
  ADD COLUMN `sell_fee_amount` decimal(32,16) DEFAULT NULL COMMENT '分摊的卖出原币手续费' AFTER `sell_fee_asset`,
  ADD COLUMN `sell_fee_quote_amount` decimal(32,16) DEFAULT NULL COMMENT '分摊的卖出折算手续费' AFTER `sell_fee_amount`,
  ADD COLUMN `fee_valuation_complete` tinyint NOT NULL DEFAULT '0' COMMENT '手续费估值是否完整' AFTER `sell_fee_quote_amount`;
