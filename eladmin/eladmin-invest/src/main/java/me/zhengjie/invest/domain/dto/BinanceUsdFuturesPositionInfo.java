package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class BinanceUsdFuturesPositionInfo implements Serializable {

    private String symbol;
    private String positionSide;
    private BigDecimal positionAmt = BigDecimal.ZERO;
    private BigDecimal entryPrice = BigDecimal.ZERO;
    private BigDecimal breakEvenPrice = BigDecimal.ZERO;
    private BigDecimal markPrice = BigDecimal.ZERO;
    private BigDecimal notional = BigDecimal.ZERO;
    private BigDecimal unrealizedPnl = BigDecimal.ZERO;
    private BigDecimal liquidationPrice = BigDecimal.ZERO;
    private Integer leverage;
    private String marginType;
    private Boolean autoAddMargin;
    private BigDecimal maxNotionalValue = BigDecimal.ZERO;
    private BigDecimal isolatedMargin = BigDecimal.ZERO;
    private BigDecimal initialMargin = BigDecimal.ZERO;
    private BigDecimal maintMargin = BigDecimal.ZERO;
    private BigDecimal positionInitialMargin = BigDecimal.ZERO;
    private BigDecimal openOrderInitialMargin = BigDecimal.ZERO;
    private String marginAsset;
    private Integer adl;
    private Long updateTime;
}
