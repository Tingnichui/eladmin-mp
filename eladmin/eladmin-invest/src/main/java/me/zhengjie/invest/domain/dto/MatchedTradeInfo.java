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
     * 盈亏
     */
    private BigDecimal pnl;
    /**
     * 回报率
     */
    private BigDecimal roi;
    /**
     * 持仓时间
     */
    private Long holdMillis;
    /**
     * 手续费
     */
    private BigDecimal fee;
    /**
     * 手续费率
     */
    private BigDecimal feeRate;
    /**
     * 净盈亏
     */
    private BigDecimal netPnl;
    /**
     * 盈亏平衡价
     */
    private BigDecimal breakEvenPrice;

    public BigDecimal getBreakEvenPrice() {
        BigDecimal qty = this.getQty();
        if (qty == null || qty.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // 每单位的手续费
        BigDecimal feePerUnit = this.getFee().divide(qty, 8, RoundingMode.HALF_UP);
        // 此时只计算了
        BigDecimal breakEvenPrice = this.side ? this.openPrice.add(feePerUnit) : this.openPrice.subtract(feePerUnit);
        return breakEvenPrice.setScale(8, RoundingMode.HALF_UP);
    }

    public BigDecimal getNetPnl() {
        return this.getPnl().subtract(this.getFee());
    }

    public BigDecimal getFee() {
        return (this.getOpenAmount().add(this.getCloseAmount())).multiply(this.feeRate);
    }

    public MatchedTradeInfo(boolean side,String feeRate) {
        this.side = side;
        this.feeRate = new BigDecimal(feeRate);
    }

    public BigDecimal getOpenAmount() {
        return qty.multiply(openPrice);
    }

    public BigDecimal getCloseAmount() {
        return qty.multiply(closePrice);
    }

    public BigDecimal getPnl() {
        BigDecimal pnl = this.getCloseAmount().subtract(this.getOpenAmount());
        return this.side ? pnl : pnl.negate();
    }

    public BigDecimal getRoi() {
        BigDecimal radio = this.getPnl().divide(this.getOpenAmount(), 4, RoundingMode.HALF_UP);
        return this.side ? radio : radio.negate();
    }

    public Long getHoldMillis() {
        if (null != this.closeTime && null != this.openTime) {
            return closeTime.getTime() - openTime.getTime();
        }
        return 0L;
    }


}
