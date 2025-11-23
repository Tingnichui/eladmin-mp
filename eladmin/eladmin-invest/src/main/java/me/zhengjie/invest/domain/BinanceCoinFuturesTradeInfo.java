/*
*  Copyright 2019-2023 Zheng Jie
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/
package me.zhengjie.invest.domain;

import lombok.Data;
import cn.hutool.core.bean.BeanUtil;
import io.swagger.annotations.ApiModelProperty;
import cn.hutool.core.bean.copier.CopyOptions;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
* @description /
* @author genghui
* @date 2025-11-23
**/
@Data
@TableName("binance_coin_futures_trade_info")
public class BinanceCoinFuturesTradeInfo implements Serializable {

    @TableId(value = "id")
    @ApiModelProperty(value = "id")
    private Long id;

    @NotNull
    @ApiModelProperty(value = "用户编号")
    private Integer uid;

    @NotBlank
    @ApiModelProperty(value = "交易对")
    private String symbol;

    @NotNull
    @ApiModelProperty(value = "订单 ID")
    private Long orderId;

    @NotNull
    @ApiModelProperty(value = "成交价格")
    private BigDecimal price;

    @NotNull
    @ApiModelProperty(value = "成交数量")
    private BigDecimal qty;

    @NotNull
    @ApiModelProperty(value = "成交额")
    private BigDecimal baseQty;

    @NotNull
    @ApiModelProperty(value = "手续费")
    private BigDecimal commission;

    @NotBlank
    @ApiModelProperty(value = "手续费计价单位")
    private String commissionAsset;

    @NotNull
    @ApiModelProperty(value = "成交时间")
    private Timestamp time;

    @NotNull
    @ApiModelProperty(value = "是否为买方")
    private Integer buyer;

    @NotNull
    @ApiModelProperty(value = "是否为挂单方")
    private Integer maker;

    @NotNull
    @ApiModelProperty(value = "实现盈亏")
    private BigDecimal realizedPnl;

    @NotBlank
    @ApiModelProperty(value = "买卖方向")
    private String side;

    @NotBlank
    @ApiModelProperty(value = "持仓方向")
    private String positionSide;

    @NotBlank
    @ApiModelProperty(value = "标的交易对")
    private String pair;

    @NotBlank
    @ApiModelProperty(value = "保证金币种")
    private String marginAsset;

    public void copy(BinanceCoinFuturesTradeInfo source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
