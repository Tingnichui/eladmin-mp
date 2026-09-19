package me.zhengjie.invest.domain.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 当前仍持有的现货买入批次及其底仓状态。
 */
@Data
public class BinanceSpotCorePositionCandidate {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long corePositionId;

    private Integer uid;

    private String symbol;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long tradeId;

    private Timestamp tradeTime;

    private BigDecimal price;

    private BigDecimal remainingQty;

    private BigDecimal coreQty;

    private BigDecimal availableQty;

    private Timestamp lockedAt;

    private String remark;
}
