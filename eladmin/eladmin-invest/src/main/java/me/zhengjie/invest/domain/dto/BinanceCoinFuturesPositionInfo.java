package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class BinanceCoinFuturesPositionInfo implements Serializable {

    private String symbol;
    private String positionSide;
    private BigDecimal positionAmt = BigDecimal.ZERO;
    private BigDecimal entryPrice = BigDecimal.ZERO;
    private BigDecimal breakEvenPrice = BigDecimal.ZERO;
    private BigDecimal markPrice = BigDecimal.ZERO;
    private BigDecimal notionalValue = BigDecimal.ZERO;
    private BigDecimal unrealizedPnl = BigDecimal.ZERO;
    private BigDecimal liquidationPrice = BigDecimal.ZERO;
    private Integer leverage;
    private String marginType;
    private Boolean autoAddMargin;
    private BigDecimal isolatedMargin = BigDecimal.ZERO;
    private BigDecimal isolatedWallet = BigDecimal.ZERO;
    private BigDecimal maxQty = BigDecimal.ZERO;
    private Long updateTime;
}
