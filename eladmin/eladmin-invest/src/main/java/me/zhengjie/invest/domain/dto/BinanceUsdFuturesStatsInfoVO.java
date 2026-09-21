package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class BinanceUsdFuturesStatsInfoVO implements Serializable {

    private BinanceUsdFuturesPositionInfo positionInfo = new BinanceUsdFuturesPositionInfo();
    private BinanceUsdFuturesTradeSummary tradeSummary = new BinanceUsdFuturesTradeSummary();
    private BinanceUsdFuturesAccountInfo accountInfo = new BinanceUsdFuturesAccountInfo();
    private List<MatchedTradeInfo> tradeList = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
