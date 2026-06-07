package me.zhengjie.invest.domain.vo;

import lombok.Data;

@Data
public class InvestTradeAnalysisOcrVO {

    private String direction;

    private String amount;

    private String openPrice;

    private String openTime;

    private String closePrice;

    private String closeTime;

    private String netProfit;
}
