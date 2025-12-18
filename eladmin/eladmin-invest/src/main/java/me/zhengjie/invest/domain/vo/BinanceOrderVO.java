package me.zhengjie.invest.domain.vo;

import lombok.Data;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BinanceOrderVO implements Serializable {

    // 开仓方向 true做多，false做空
    @NotNull
    private Boolean posDir;
    // 开仓数量
    @NotNull
    @DecimalMin(value = "0.001", inclusive = true, message = "开仓数量必须大于 0.001")
    @DecimalMax(value = "0.01", inclusive = true, message = "开仓数量不能超过 0.01")
    private BigDecimal posQty;
    // 开仓价格
    @NotNull
    private BigDecimal openPrice;
    // 止损价格
    @NotNull
    private BigDecimal stopLossPrice;

}
