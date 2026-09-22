package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 快捷卖出订单的持仓来源，用于展示、可卖数量控制和成交后的限定范围撮合。
 */
@Data
public class BinanceSpotSellSourceDto implements Serializable {

    public enum SourceType {
        ORDER,
        TRADE
    }

    private Integer uid;
    private String symbol;
    private Long sellOrderId;
    private SourceType sourceType;
    private Long sourceOrderId;
    private Long sourceTradeId;
    private BigDecimal quantity;
    private Long createdAt;
    private String status;
    private BigDecimal executedQty;
    private Long updatedAt;
}
