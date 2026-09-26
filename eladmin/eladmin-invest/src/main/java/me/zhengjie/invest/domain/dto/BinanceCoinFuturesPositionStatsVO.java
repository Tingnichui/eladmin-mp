package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class BinanceCoinFuturesPositionStatsVO implements Serializable {

    private boolean realtime = true;
    private BinanceCoinFuturesPositionInfo positionInfo = new BinanceCoinFuturesPositionInfo();
    private BinanceCoinFuturesContractInfo contractInfo = new BinanceCoinFuturesContractInfo();
    private List<BinanceCoinFuturesOpenTradeInfo> tradeList = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
