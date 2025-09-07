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

import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.annotation.TableField;
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
* @date 2025-08-02
**/
@Data
@TableName("binance_trade_info")
public class BinanceTradeInfo implements Serializable {

    @TableId(value = "id")
    @ApiModelProperty(value = "id")
    private Long id;

    @NotBlank
    @ApiModelProperty(value = "交易对")
    private String symbol;

    @NotNull
    @ApiModelProperty(value = "成交价格")
    private BigDecimal price;

    @NotNull
    @ApiModelProperty(value = "成交数量")
    private BigDecimal qty;

    @NotNull
    @ApiModelProperty(value = "手续费")
    private BigDecimal commission;

    @NotNull
    @ApiModelProperty(value = "成交时间")
    private Timestamp time;

    @NotNull
    @ApiModelProperty(value = "订单 ID")
    private Long orderId;

    @NotNull
    @ApiModelProperty(value = "成交额")
    private BigDecimal quoteQty;

    @NotBlank
    @ApiModelProperty(value = "手续费资产")
    private String commissionAsset;

    @NotNull
    @ApiModelProperty(value = "是否为买方")
    private Integer isBuyer;

    @NotNull
    @ApiModelProperty(value = "是否为挂单方")
    private Integer isMaker;

    @NotNull
    @ApiModelProperty(value = "是否为最佳匹配")
    private Integer isBestMatch;

    @NotNull
    @ApiModelProperty(value = "用户编号")
    private Integer uid;

    @TableField(exist = false)
    private String idCardName;

    @TableField(exist = false)
    private Integer hedgedFlag = 0;

    public BigDecimal getAmount() {
        return NumberUtil.mul(this.price, this.qty);
    }

    public void copy(BinanceTradeInfo source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
