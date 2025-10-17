package me.zhengjie.invest.domain.vo;

import lombok.Data;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BinanceFuturesTradeStatsInfoVO implements Serializable {
    //开仓均价
    private BigDecimal avgOpenPrice;
    //平仓均价
    private BigDecimal avgClosePrice;
    //总的利润
    private BigDecimal profit;
    //收益率
    private BigDecimal profitPct;
    //剩余未平仓总金额
    private BigDecimal totalWaitSellAmount;
    //剩余未平仓总数量
    private BigDecimal totalWaitSellQty;
    //剩余未平仓均价
    private BigDecimal totalWaitAvgSellPrice;

}
