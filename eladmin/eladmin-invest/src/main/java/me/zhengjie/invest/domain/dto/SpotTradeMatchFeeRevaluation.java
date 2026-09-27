package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpotTradeMatchFeeRevaluation {

    private Long id;
    private Long buyTradeId;
    private Long sellTradeId;
    private BigDecimal matchedQty;
    private BigDecimal pnl;
    private BigDecimal buyTradeQty;
    private BigDecimal buyCommission;
    private String buyCommissionAsset;
    private BigDecimal buyCommissionQuoteAmount;
    private String buyCommissionValuationStatus;
    private BigDecimal sellTradeQty;
    private BigDecimal sellCommission;
    private String sellCommissionAsset;
    private BigDecimal sellCommissionQuoteAmount;
    private String sellCommissionValuationStatus;
}
