package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class BinanceUsdFuturesAccountInfo implements Serializable {

    private BigDecimal walletBalance = BigDecimal.ZERO;
    private BigDecimal unrealizedProfit = BigDecimal.ZERO;
    private BigDecimal marginBalance = BigDecimal.ZERO;
    private BigDecimal availableBalance = BigDecimal.ZERO;
    private BigDecimal initialMargin = BigDecimal.ZERO;
    private BigDecimal maintMargin = BigDecimal.ZERO;
}
