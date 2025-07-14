package me.zhengjie.invest.domain.vo;

import com.alibaba.fastjson.JSON;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.sql.Timestamp;


@Data
public class TradingViewNotify {

    @NotBlank
    private String secret;

    /**
     * 交易时间
     */
    private Timestamp tradeTime;

    /**
     * 交易品种
     */
    private String symbol;

    /**
     * K线周期
     */
    private String period;

    /**
     * 交易策略
     */
    private String tradingStrategy;

    /**
     * 交易方向
     */
    private String tradeDirection;

    /**
     * 开仓价格
     */
    private BigDecimal openPrice;

    @Override
    public String toString() {
        return tradeTime + "，" + symbol + " 在 " + period + " K线，出现 " + tradingStrategy + " 策略的 " + tradeDirection + " 信号，开仓价格为： " + openPrice;
    }

    public static void main(String[] args) {
        TradingViewNotify object = new TradingViewNotify();
        object.setSecret("13123");
        object.setTradeTime(new Timestamp(System.currentTimeMillis()));
        object.setSymbol("BTCUSDT");
        object.setPeriod("15MIN");
        object.setTradingStrategy("MACD");
        object.setTradeDirection("BUY");
        object.setOpenPrice(new BigDecimal("120000.01"));
        System.err.println(JSON.toJSONString(object));
        System.err.println(object);
    }

}