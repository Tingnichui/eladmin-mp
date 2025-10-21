package me.zhengjie.invest.domain.vo;

import lombok.Data;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BinanceFuturesTradeStatsInfoVO implements Serializable {
    // 利润
    private BigDecimal profit = BigDecimal.ZERO;
    // 手续费
    private BigDecimal fee = BigDecimal.ZERO;
    //剩余未平仓总金额
    private BigDecimal totalWaitSellAmount = BigDecimal.ZERO;
    //剩余未平仓总数量
    private BigDecimal totalWaitSellQty = BigDecimal.ZERO;
    //剩余未平仓均价
    private BigDecimal totalWaitAvgSellPrice = BigDecimal.ZERO;

}
