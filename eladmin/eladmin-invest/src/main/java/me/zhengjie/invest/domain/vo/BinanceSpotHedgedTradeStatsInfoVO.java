package me.zhengjie.invest.domain.vo;

import lombok.Data;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BinanceSpotHedgedTradeStatsInfoVO implements Serializable {

    //锁仓总金额
    private BigDecimal hedgedAmount = BigDecimal.ZERO;
    //锁仓总数量
    private BigDecimal hedgedQty = BigDecimal.ZERO;
    //锁仓均价
    private BigDecimal hedgedAvgPrice = BigDecimal.ZERO;

}
