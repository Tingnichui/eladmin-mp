package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 币本位合约订单响应。
 */
@Data
public class BinanceCoinFuturesOrderDto {

    private String symbol;
    private String pair;
    private Long orderId;
    private String clientOrderId;
    private BigDecimal price;
    private BigDecimal avgPrice;
    private BigDecimal origQty;
    private BigDecimal executedQty;
    private BigDecimal cumBase;
    private String status;
    private String timeInForce;
    private String type;
    private String origType;
    private String side;
    private String positionSide;
    private Boolean reduceOnly;
    private Boolean closePosition;
    private Long time;
    private Long updateTime;
}
