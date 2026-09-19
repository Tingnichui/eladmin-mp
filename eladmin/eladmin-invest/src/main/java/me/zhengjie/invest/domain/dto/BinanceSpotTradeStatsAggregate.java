package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 现货数据库撮合汇总结果。
 */
@Data
public class BinanceSpotTradeStatsAggregate {

    private BigDecimal totalBuyAmount = BigDecimal.ZERO;

    private BigDecimal totalSellAmount = BigDecimal.ZERO;

    private BigDecimal pnl = BigDecimal.ZERO;

    private BigDecimal fee = BigDecimal.ZERO;

    private BigDecimal netPnl = BigDecimal.ZERO;
}
