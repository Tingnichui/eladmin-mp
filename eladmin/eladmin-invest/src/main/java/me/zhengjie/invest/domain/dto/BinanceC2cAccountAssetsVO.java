package me.zhengjie.invest.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class BinanceC2cAccountAssetsVO {

    @ApiModelProperty(value = "币安账户用户编号")
    private Integer uid;

    @ApiModelProperty(value = "已完成买入订单数量")
    private Long orderCount;

    @ApiModelProperty(value = "人民币总额")
    private BigDecimal rmbAmount;

    @ApiModelProperty(value = "数字资产总额")
    private BigDecimal usdAmount;

    @ApiModelProperty(value = "人民币兑数字资产平均汇率")
    private BigDecimal rmbToUsdRate;

    @ApiModelProperty(value = "最后同步时间")
    private Timestamp syncTime;
}
