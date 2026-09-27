package me.zhengjie.invest.domain.dto;

import lombok.Data;

@Data
public class SpotActualFeeRebuildResult {

    private int tradeCount;
    private int valuedCount;
    private int failedCount;
    private int adjustedCorePositionCount;
    private int coreConflictCount;
    private int deletedMatchCount;
    private int deletedStateCount;
    private BinanceSpotTradeMatchResult matchResult;
    private boolean rebuilt;
}
