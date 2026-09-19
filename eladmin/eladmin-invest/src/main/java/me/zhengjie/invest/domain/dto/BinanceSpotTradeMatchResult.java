package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 现货数据库撮合执行结果。
 */
@Data
public class BinanceSpotTradeMatchResult {

    private int initializedCount;

    private int matchCount;

    private int completedSellCount;

    private int exceptionSellCount;

    private BigDecimal matchedQty = BigDecimal.ZERO;

    public void addMatch(BigDecimal qty) {
        matchCount++;
        matchedQty = matchedQty.add(qty);
    }
}
