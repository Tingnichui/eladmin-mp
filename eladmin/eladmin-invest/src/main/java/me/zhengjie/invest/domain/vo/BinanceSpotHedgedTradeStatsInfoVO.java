package me.zhengjie.invest.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class BinanceSpotHedgedTradeStatsInfoVO implements Serializable {

    //锁仓总金额
    private BigDecimal posAmount = BigDecimal.ZERO;
    //锁仓总数量
    private BigDecimal posQty = BigDecimal.ZERO;
    //锁仓均价
    private BigDecimal posAvgPrice = BigDecimal.ZERO;

}
