package me.zhengjie.invest.domain.dto;

import lombok.Data;

@Data
public class SpotActualFeeRebuildResult {

    private int tradeCount;
    private int valuedCount;
    private int failedCount;
    private int updatedMatchCount;
    private boolean rebuilt;
}
