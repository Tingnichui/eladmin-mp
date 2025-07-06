package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class MatchedTradeInfo {

    private BigDecimal qty;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private Timestamp buyTime;
    private Timestamp sellTime;

    // 计算持仓秒数
    public long getHoldingSeconds() {
        long diffMillis = sellTime.getTime() - buyTime.getTime();
        return diffMillis > 0 ? diffMillis / 1000 : 0;
    }

    // 计算买入金额
    public BigDecimal getBuyAmount() {
        return qty.multiply(buyPrice);
    }

    public BigDecimal getSellAmount() {
        return qty.multiply(sellPrice);
    }

}
