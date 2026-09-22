package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class BinanceCoinFuturesOpenTradeInfo implements Serializable {

    private Long tradeId;
    private Long orderId;
    private BigDecimal contractQty = BigDecimal.ZERO;
    private BigDecimal baseQty = BigDecimal.ZERO;
    private BigDecimal openPrice = BigDecimal.ZERO;
    private Timestamp openTime;
    private BigDecimal markPrice = BigDecimal.ZERO;
    private BigDecimal unrealizedPnl = BigDecimal.ZERO;
    private BigDecimal roi;
}
