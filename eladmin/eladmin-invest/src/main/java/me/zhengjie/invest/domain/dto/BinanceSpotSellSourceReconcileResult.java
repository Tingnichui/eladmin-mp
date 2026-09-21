package me.zhengjie.invest.domain.dto;

import lombok.Data;

/**
 * 币安现货卖出来源关联对账结果。
 */
@Data
public class BinanceSpotSellSourceReconcileResult {

    private int openCount;
    private int checkedCount;
    private int retainedCount;
    private int filledCount;
    private int removedCount;
    private int failedCount;
}
