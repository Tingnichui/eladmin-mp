package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;

@Data
public class MatchedTradeInfo {

    /**
     * 开仓方向，true做多 false做空
     */
    private boolean side;
    /**
     * 成交数量
     */
    private BigDecimal qty;
    /**
     * 开仓价格
     */
    private BigDecimal openPrice;
    /**
     * 平仓价格
     */
    private BigDecimal closePrice;
    /**
     * 开仓时间
     */
    private Timestamp openTime;
    /**
     * 平仓时间
     */
    private Timestamp closeTime;

    /**
     * 开仓金额
     */
    private BigDecimal openAmount;
    /**
     * 平仓金额
     */
    private BigDecimal closeAmount;
    /**
     * 实现盈亏
     */
    private BigDecimal realizedPnl;
    /**
     * 盈亏率
     */
    private BigDecimal pnlRatio;
    private Long holdMillis;


    public MatchedTradeInfo(boolean side) {
        this.side = side;
    }

    public BigDecimal getOpenAmount() {
        return qty.multiply(openPrice);
    }

    public BigDecimal getCloseAmount() {
        return qty.multiply(closePrice);
    }

    public BigDecimal getRealizedPnl() {
        BigDecimal pnl = this.getCloseAmount().subtract(this.getOpenAmount());
        return this.side ? pnl : pnl.negate();
    }

    public BigDecimal getPnlRatio() {
        BigDecimal radio = this.getRealizedPnl().divide(this.getOpenAmount(), 4, RoundingMode.HALF_UP);
        return this.side ? radio : radio.negate();
    }

    public Long getHoldMillis() {
        if (null != this.closeTime && null != this.openTime) {
            return closeTime.getTime() - openTime.getTime();
        }
        return 0L;
    }


}
