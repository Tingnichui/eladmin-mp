package me.zhengjie.invest.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量锁定现货底仓请求。
 */
@Data
public class BinanceSpotCorePositionBatchLockRequest {

    @NotNull
    @ApiModelProperty(value = "币安账户用户编号", required = true)
    private Integer uid;

    @NotBlank
    @ApiModelProperty(value = "现货交易对", required = true)
    private String symbol;

    @NotEmpty
    @ApiModelProperty(value = "原始现货买入成交 ID 列表", required = true)
    private List<Long> tradeIds;
}
