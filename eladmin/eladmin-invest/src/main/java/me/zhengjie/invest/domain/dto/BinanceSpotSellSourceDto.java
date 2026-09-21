package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 快捷卖出订单的操作来源，仅用于展示，不改变 FIFO 撮合关系。
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
