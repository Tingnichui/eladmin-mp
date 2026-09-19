package me.zhengjie.invest.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 调整现货底仓请求。
 */
@Data
public class BinanceSpotCorePositionAdjustRequest {

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @ApiModelProperty(value = "调整后的底仓数量", required = true)
    private BigDecimal coreQty;

    @ApiModelProperty(value = "备注")
    private String remark;
}
