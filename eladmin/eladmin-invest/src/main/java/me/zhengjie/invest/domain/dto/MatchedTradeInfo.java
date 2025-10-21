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

    public void computeDerivedFields() {
        this.buyAmount = qty.multiply(buyPrice);
        this.sellAmount = qty.multiply(sellPrice);
        this.profit = this.sellAmount.subtract(this.buyAmount);
        this.profitRate = this.profit.divide(this.buyAmount, 4, RoundingMode.HALF_UP);
        if (null != this.sellTime && null != this.buyTime) {
            this.holdMillis = sellTime.getTime() - buyTime.getTime();
        }
    }


}
