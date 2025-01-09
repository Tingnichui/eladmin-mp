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

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import cn.hutool.core.bean.BeanUtil;
import io.swagger.annotations.ApiModelProperty;
import cn.hutool.core.bean.copier.CopyOptions;
import java.sql.Timestamp;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import me.zhengjie.base.BaseEntity;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
* @description /
* @author genghui
* @date 2025-01-04
**/
@Data
@TableName("invest_trade_record")
public class InvestTradeRecord extends BaseEntity implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "id")
    private Integer id;

    @NotNull
    @ApiModelProperty(value = "投资产品")
    private Integer productId;

    @ApiModelProperty(value = "投资产品名称")
    @TableField(exist = false)
    private String investProductName;

    @NotNull
    @ApiModelProperty(value = "交易类型")
    private Integer tradeType;

    @NotNull
    @Min(value = 1)
    @ApiModelProperty(value = "交易数量")
    private Integer tradeNum;

    @NotNull
    @Min(value = 1)
    @ApiModelProperty(value = "开仓价格")
    private Long openPrice;

    @ApiModelProperty(value = "建仓时间")
    private Timestamp openTime;

    @NotNull
    @Min(value = 1)
    @ApiModelProperty(value = "杠杆")
    private Integer leverage;

    @NotNull
    @Min(value = 1)
    @ApiModelProperty(value = "开仓成本")
    private Long cost;

    @NotNull
    @Min(value = 1)
    @ApiModelProperty(value = "止损价格")
    private Long stopLoss;

    @Min(value = 0)
    @ApiModelProperty(value = "止盈价格")
    private Long takeProfit;

    @Min(value = 1)
    @ApiModelProperty(value = "平仓价格")
    private Long closePrice;

    @ApiModelProperty(value = "平仓时间")
    private Timestamp closeTime;

    @ApiModelProperty(value = "收益")
    private Long profit;

    @NotNull
    @ApiModelProperty(value = "交易状态")
    private Integer operateStatus;

    @NotBlank
    @ApiModelProperty(value = "分析")
    private String analysis;

    @ApiModelProperty(value = "复盘")
    private String review;

    public void copy(InvestTradeRecord source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
