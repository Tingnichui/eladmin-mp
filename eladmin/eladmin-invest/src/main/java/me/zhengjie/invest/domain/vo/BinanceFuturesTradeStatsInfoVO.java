package me.zhengjie.invest.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class BinanceFuturesTradeStatsInfoVO implements Serializable {
    // 利润
    private BigDecimal profit = BigDecimal.ZERO;
    // 手续费
    private BigDecimal fee = BigDecimal.ZERO;
    //剩余未平仓总金额
    private BigDecimal totalWaitCloseAmount = BigDecimal.ZERO;
    //剩余未平仓总数量
    private BigDecimal totalWaitCloseQty = BigDecimal.ZERO;
    //剩余未平仓均价
    private BigDecimal totalWaitAvgClosePrice = BigDecimal.ZERO;

}
