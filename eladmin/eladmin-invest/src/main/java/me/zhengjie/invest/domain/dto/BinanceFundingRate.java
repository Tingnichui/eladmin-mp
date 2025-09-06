package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.util.Date;

@Data
public class BinanceFundingRate {

    /**
     * 交易对，BTCUSDT
     */
    private String symbol;

    /**
     * 资金费率，-0.03750000
     */
    private String fundingRate;

    /**
     * 资金费时间，1570608000000
     */
    private Long fundingTime;

    /**
     * 资金费对应标记价格，34287.54619963
     */
    private String markPrice;

    public Date getFundingTime() {
        return new Date(this.fundingTime);
    }
}
