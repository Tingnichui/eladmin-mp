package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class BinanceCoinFuturesContractInfo implements Serializable {

    private String symbol;
    private String pair;
    private String contractType;
    private BigDecimal contractSize = BigDecimal.ZERO;
    private String baseAsset;
    private String quoteAsset;
    private String marginAsset;
    private Integer pricePrecision;
    private Integer quantityPrecision;
}
