package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class BinanceCoinFuturesStatsInfoVO implements Serializable {

    private BinanceCoinFuturesPositionInfo positionInfo = new BinanceCoinFuturesPositionInfo();
    private BinanceCoinFuturesTradeSummary tradeSummary = new BinanceCoinFuturesTradeSummary();
    private BinanceCoinFuturesAccountInfo accountInfo = new BinanceCoinFuturesAccountInfo();
    private BinanceCoinFuturesContractInfo contractInfo = new BinanceCoinFuturesContractInfo();
    private List<BinanceCoinFuturesOpenTradeInfo> tradeList = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
