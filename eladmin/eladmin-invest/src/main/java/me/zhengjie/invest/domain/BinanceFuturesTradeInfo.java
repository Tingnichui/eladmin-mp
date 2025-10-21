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
* @date 2025-10-17
**/
@Data
@TableName("binance_futures_trade_info")
public class BinanceFuturesTradeInfo implements Serializable {

    @TableId(value = "id")
    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "用户编号")
    private Integer uid;

    @ApiModelProperty(value = "交易对")
    private String symbol;

    @ApiModelProperty(value = "订单 ID")
    private Long orderId;

    @ApiModelProperty(value = "成交价格")
    private BigDecimal price;

    @ApiModelProperty(value = "成交数量")
    private BigDecimal qty;

    @ApiModelProperty(value = "成交额")
    private BigDecimal quoteQty;

    @ApiModelProperty(value = "手续费")
    private BigDecimal commission;

    @ApiModelProperty(value = "手续费计价单位")
    private String commissionAsset;

    @ApiModelProperty(value = "成交时间")
    private Timestamp time;

    @ApiModelProperty(value = "是否为买方")
    private Integer buyer;

    @ApiModelProperty(value = "是否为挂单方")
    private Integer maker;

    @ApiModelProperty(value = "实现盈亏")
    private BigDecimal realizedPnl;

    @ApiModelProperty(value = "买卖方向")
    private String side;

    @ApiModelProperty(value = "持仓方向")
    private String positionSide;

    public void copy(BinanceFuturesTradeInfo source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
