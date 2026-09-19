/*
*  Copyright 2019-2025 Zheng Jie
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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.sql.Timestamp;
import java.math.BigDecimal;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
* @description /
* @author genghui
* @date 2026-09-19
**/
@Data
@TableName("binance_spot_trade_match")
public class BinanceSpotTradeMatch implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "主键")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @NotNull
    @ApiModelProperty(value = "币安账户用户编号")
    private Integer uid;

    @NotBlank
    @ApiModelProperty(value = "现货交易对")
    private String symbol;

    @NotNull
    @ApiModelProperty(value = "买入成交 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long buyTradeId;

    @NotNull
    @ApiModelProperty(value = "卖出成交 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sellTradeId;

    @NotNull
    @ApiModelProperty(value = "撮合数量")
    private BigDecimal matchedQty;

    @NotNull
    @ApiModelProperty(value = "买入价格快照")
    private BigDecimal buyPrice;

    @NotNull
    @ApiModelProperty(value = "卖出价格快照")
    private BigDecimal sellPrice;

    @NotNull
    @ApiModelProperty(value = "买入成交时间")
    private Timestamp buyTime;

    @NotNull
    @ApiModelProperty(value = "卖出成交时间")
    private Timestamp sellTime;

    @NotNull
    @ApiModelProperty(value = "撮合买入金额")
    private BigDecimal buyAmount;

    @NotNull
    @ApiModelProperty(value = "撮合卖出金额")
    private BigDecimal sellAmount;

    @NotNull
    @ApiModelProperty(value = "手续费率")
    private BigDecimal feeRate;

    @NotNull
    @ApiModelProperty(value = "已实现盈亏")
    private BigDecimal pnl;

    @NotNull
    @ApiModelProperty(value = "手续费")
    private BigDecimal fee;

    @NotNull
    @ApiModelProperty(value = "扣除手续费后的净盈亏")
    private BigDecimal netPnl;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    public void copy(BinanceSpotTradeMatch source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
