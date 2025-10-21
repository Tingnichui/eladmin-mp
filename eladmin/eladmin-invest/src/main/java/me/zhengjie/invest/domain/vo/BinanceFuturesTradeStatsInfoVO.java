package me.zhengjie.invest.domain.vo;

import lombok.Data;
import me.zhengjie.invest.domain.BinanceFuturesTradeInfo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BinanceFuturesTradeStatsInfoVO implements Serializable {
    // 利润
    private BigDecimal profit = BigDecimal.ZERO;
    // 手续费
    private BigDecimal fee = BigDecimal.ZERO;
    //锁仓总金额
    private BigDecimal hedgedAmount = BigDecimal.ZERO;
    //锁仓总数量
    private BigDecimal hedgedQty = BigDecimal.ZERO;
    //锁仓均价
    private BigDecimal hedgedAvgPrice = BigDecimal.ZERO;
    //剩余未平仓总金额
    private BigDecimal totalWaitCloseAmount = BigDecimal.ZERO;
    //剩余未平仓总数量
    private BigDecimal totalWaitCloseQty = BigDecimal.ZERO;
    //剩余未平仓均价
    private BigDecimal totalWaitAvgClosePrice = BigDecimal.ZERO;
    // 未匹配到止损的交易
    private List<BinanceFuturesTradeInfo> noStopLossTradeInfoList;

}
