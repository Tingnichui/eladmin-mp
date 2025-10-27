package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;

@Data
public class MatchedTradeInfo {

    private BigDecimal qty;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private Timestamp buyTime;
    private Timestamp sellTime;

    // 以下字段通过基础信息计算
    private BigDecimal buyAmount;
    private BigDecimal sellAmount;
    private BigDecimal profit;
    private BigDecimal profitRate;
    private Long holdMillis;


    public BigDecimal getBuyAmount() {
        return qty.multiply(buyPrice);
    }

    public BigDecimal getSellAmount() {
        return qty.multiply(sellPrice);
    }

    public BigDecimal getProfit() {
        return this.getSellAmount().subtract(this.getBuyAmount());
    }

    public BigDecimal getProfitRate() {
        return this.getProfit().divide(this.getBuyAmount(), 4, RoundingMode.HALF_UP);
    }

    public Long getHoldMillis() {
        if (null != this.sellTime && null != this.buyTime) {
            return sellTime.getTime() - buyTime.getTime();
        }
        return 0L;
    }


}
