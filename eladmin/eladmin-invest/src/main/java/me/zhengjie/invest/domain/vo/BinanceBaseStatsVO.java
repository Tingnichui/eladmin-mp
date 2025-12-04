package me.zhengjie.invest.domain.vo;

import lombok.Data;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BinanceBaseStatsVO implements Serializable {

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
    // 交易
    private List<MatchedTradeInfo> tradeList;

}
