package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class BinanceCoinFuturesTradeSummary implements Serializable {

    private BigDecimal realizedPnl = BigDecimal.ZERO;
    private BigDecimal commission = BigDecimal.ZERO;
    private BigDecimal fundingFee = BigDecimal.ZERO;
    private BigDecimal netPnl = BigDecimal.ZERO;
    private int totalTradeCount;
    private int openTradeCount;
    private int closedTradeCount;
    private int closedPositionCount;
}
