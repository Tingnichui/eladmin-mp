package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 币安现货当前挂单。
 */
@Data
public class BinanceSpotOpenOrderDto {

    private String symbol;
    private Long orderId;
    private String clientOrderId;
    private BigDecimal price;
    private BigDecimal origQty;
    private BigDecimal executedQty;
    private String status;
    private String timeInForce;
    private String type;
    private String side;
    private BigDecimal stopPrice;
    private Long time;
    private Long updateTime;
    private Boolean isWorking;
    private String pegPriceType;
    private String pegOffsetType;
    private Integer pegOffsetValue;
    private BigDecimal peggedPrice;
}
