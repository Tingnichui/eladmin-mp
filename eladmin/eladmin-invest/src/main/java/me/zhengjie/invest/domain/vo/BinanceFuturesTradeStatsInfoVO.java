package me.zhengjie.invest.domain.vo;

import lombok.Data;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BinanceFuturesTradeStatsInfoVO extends BinanceBaseStatsVO implements Serializable {
    //上一次净盈亏
    private BigDecimal lastNetPnl;
    //锁仓总金额
    private BigDecimal hedgedAmount = BigDecimal.ZERO;
    //锁仓总数量
    private BigDecimal hedgedQty = BigDecimal.ZERO;
    //锁仓均价
    private BigDecimal hedgedAvgPrice = BigDecimal.ZERO;
    // 止损金额
    private BigDecimal stopLossAmount = BigDecimal.ZERO;
    // 开仓交易
    private List<MatchedTradeInfo> openTradeList;

}
