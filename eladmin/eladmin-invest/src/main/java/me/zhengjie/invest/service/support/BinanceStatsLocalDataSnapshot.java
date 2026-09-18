package me.zhengjie.invest.service.support;

import lombok.Data;
import me.zhengjie.invest.domain.BinanceCoinFuturesTradeInfo;
import me.zhengjie.invest.domain.BinanceFuturesTradeInfo;
import me.zhengjie.invest.domain.BinanceTradeInfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class BinanceStatsLocalDataSnapshot {

    private List<BinanceTradeInfo> spotTrades = new ArrayList<>();
    private List<BinanceFuturesTradeInfo> usdFuturesTrades = new ArrayList<>();
    private List<BinanceCoinFuturesTradeInfo> coinFuturesTrades = new ArrayList<>();
    private Date coinPositionStartTime;
    private final Map<String, Long> queryElapsedMillis = new LinkedHashMap<>();
    private final List<String> warnings = new ArrayList<>();
}
