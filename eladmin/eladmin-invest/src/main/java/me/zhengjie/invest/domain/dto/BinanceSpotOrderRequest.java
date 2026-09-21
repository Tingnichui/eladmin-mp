package me.zhengjie.invest.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import me.zhengjie.invest.constants.BinanceEnum;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 现货限价止盈止损下单请求。
 */
@Data
@ApiModel("现货限价止盈止损下单请求")
public class BinanceSpotOrderRequest implements Serializable {

    @NotNull(message = "请选择账户")
    @ApiModelProperty(value = "币安账户 UID", required = true)
    private Integer uid;

    @NotBlank(message = "请选择交易对")
    @ApiModelProperty(value = "现货交易对，如 BTCUSDT", required = true)
    private String symbol;

    @NotNull(message = "请选择买卖方向")
    @ApiModelProperty(value = "买卖方向：BUY、SELL", required = true)
    private BinanceEnum.SIDE side;

    @NotNull(message = "请选择订单类型")
    @ApiModelProperty(value = "订单类型：STOP_LOSS_LIMIT、TAKE_PROFIT_LIMIT", required = true)
    private BinanceEnum.TYPE type;

    @NotNull(message = "请输入下单数量")
    @DecimalMin(value = "0", inclusive = false, message = "下单数量必须大于 0")
    @ApiModelProperty(value = "下单数量", required = true)
    private BigDecimal quantity;

    @NotNull(message = "请输入触发价")
    @DecimalMin(value = "0", inclusive = false, message = "触发价必须大于 0")
    @ApiModelProperty(value = "触发价", required = true)
    private BigDecimal stopPrice;

    @NotNull(message = "请选择委托价模式")
    @ApiModelProperty(value = "委托价模式：FIXED、OPPONENT_FIRST", required = true)
    private BinanceEnum.PRICE_MODE priceMode;

    @DecimalMin(value = "0", inclusive = false, message = "委托价必须大于 0")
    @ApiModelProperty(value = "固定委托价；priceMode=FIXED 时必填")
    private BigDecimal price;

    @ApiModelProperty("快捷卖出来源类型：ORDER、TRADE；普通下单不传")
    private BinanceSpotSellSourceDto.SourceType sourceType;

    @ApiModelProperty("发起快捷卖出的原买入订单 ID")
    private Long sourceOrderId;

    @ApiModelProperty("发起快捷卖出的原买入成交 ID；sourceType=TRADE 时必填")
    private Long sourceTradeId;
}
