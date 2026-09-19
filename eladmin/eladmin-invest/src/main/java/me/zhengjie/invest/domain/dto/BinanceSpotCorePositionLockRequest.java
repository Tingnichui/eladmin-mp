package me.zhengjie.invest.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 锁定现货底仓请求。
 */
@Data
public class BinanceSpotCorePositionLockRequest {

    @NotNull
    @ApiModelProperty(value = "币安账户用户编号", required = true)
    private Integer uid;

    @NotBlank
    @ApiModelProperty(value = "现货交易对", required = true)
    private String symbol;

    @NotNull
    @ApiModelProperty(value = "原始现货买入成交 ID", required = true)
    private Long tradeId;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @ApiModelProperty(value = "锁定为底仓的数量", required = true)
    private BigDecimal coreQty;

    @ApiModelProperty(value = "备注")
    private String remark;
}
