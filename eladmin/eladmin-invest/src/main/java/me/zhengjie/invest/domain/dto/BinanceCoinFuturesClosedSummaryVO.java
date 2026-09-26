package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class BinanceCoinFuturesClosedSummaryVO implements Serializable {

    private BinanceCoinFuturesTradeSummary tradeSummary = new BinanceCoinFuturesTradeSummary();
    private Date firstTradeTime;
    private Date lastClosedTradeTime;
    private List<String> warnings = new ArrayList<>();
}
