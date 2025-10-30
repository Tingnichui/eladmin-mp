package me.zhengjie.invest.domain.vo;

import lombok.Data;
import me.zhengjie.invest.domain.BinanceFuturesTradeInfo;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BinanceFuturesTradeStatsInfoVO implements Serializable {
    // 持仓数量
    private BigDecimal posQty;
    // 持仓均价
    private BigDecimal posAvgPrice;
    // 持仓金额
    private BigDecimal posAmount;
    // 盈亏
    private BigDecimal pnl = BigDecimal.ZERO;
    // 手续费
    private BigDecimal fee = BigDecimal.ZERO;
    // 净盈亏
    private BigDecimal netPnl = BigDecimal.ZERO;
    //锁仓总金额
    private BigDecimal hedgedAmount = BigDecimal.ZERO;
    //锁仓总数量
    private BigDecimal hedgedQty = BigDecimal.ZERO;
    //锁仓均价
    private BigDecimal hedgedAvgPrice = BigDecimal.ZERO;
    // 未匹配到止损的交易
    private List<BinanceFuturesTradeInfo> noStopLossTradeInfoList;
    // 止损金额
    private BigDecimal stopLossAmount = BigDecimal.ZERO;
    // 现货止损对冲交易
    private List<MatchedTradeInfo> stopLossMatchTradeInfoList;

}
