package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpotFeeAssetSummary {

    private String asset;

    private BigDecimal amount;

    private BigDecimal quoteAmount;
}
