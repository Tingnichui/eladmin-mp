package me.zhengjie.invest.domain.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;


@Data
public class TradingViewNotify {

    @NotBlank
    private String secret;

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


}