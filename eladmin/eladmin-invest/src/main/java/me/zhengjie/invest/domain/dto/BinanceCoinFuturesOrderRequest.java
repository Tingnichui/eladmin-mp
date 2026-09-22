package me.zhengjie.invest.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 币本位合约普通订单请求。方向由业务动作和持仓方向共同确定，避免前端直接拼装 BUY/SELL。
 */
@Data
@ApiModel("币本位合约下单请求")
public class BinanceCoinFuturesOrderRequest implements Serializable {

    @NotNull(message = "请选择账户")
    @ApiModelProperty(value = "币安账户 UID", required = true)
    private Integer uid;

    @NotBlank(message = "请选择合约")
    @ApiModelProperty(value = "合约，如 BTCUSD_PERP", required = true)
    private String symbol;

    @NotBlank(message = "请选择开仓或平仓")
    @ApiModelProperty(value = "OPEN、CLOSE", required = true)
    private String action;

    @NotBlank(message = "请选择持仓方向")
    @ApiModelProperty(value = "LONG、SHORT", required = true)
    private String positionSide;

    @NotBlank(message = "请选择订单类型")
    @ApiModelProperty(value = "MARKET、LIMIT", required = true)
    private String type;

    @NotNull(message = "请输入下单张数")
    @DecimalMin(value = "0", inclusive = false, message = "下单张数必须大于 0")
    @ApiModelProperty(value = "合约张数", required = true)
    private BigDecimal quantity;

    @DecimalMin(value = "0", inclusive = false, message = "委托价必须大于 0")
    @ApiModelProperty("限价单委托价")
    private BigDecimal price;

    @ApiModelProperty("限价单有效方式：GTC、IOC、FOK、GTX")
    private String timeInForce;
}
