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
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
* @description /
* @author genghui
* @date 2025-07-30
**/
@Data
@TableName("invest_klines_record")
public class InvestKlinesRecord implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "id")
    private Long id;

    @NotBlank
    @ApiModelProperty(value = "交易对")
    private String symbol;

    @NotNull
    @ApiModelProperty(value = "周期，单位分钟")
    private Integer period;

    @NotNull
    @ApiModelProperty(value = "开盘时间")
    private Long openTime;

    @NotNull
    @ApiModelProperty(value = "收盘时间")
    private Long closeTime;

    @NotNull
    @ApiModelProperty(value = "开盘价")
    private BigDecimal openPrice;

    @NotNull
    @ApiModelProperty(value = "收盘价")
    private BigDecimal closePrice;

    @NotNull
    @ApiModelProperty(value = "最高价")
    private BigDecimal highPrice;

    @NotNull
    @ApiModelProperty(value = "最低价")
    private BigDecimal lowPrice;

    @NotNull
    @ApiModelProperty(value = "成交量")
    private BigDecimal volume;

    @ApiModelProperty(value = "成交额")
    private BigDecimal turnover;

    @ApiModelProperty(value = "成交笔数")
    private Integer tradeCount;

    @ApiModelProperty(value = "主动买入成交量")
    private BigDecimal buyVolume;

    @ApiModelProperty(value = "主动买入成交额")
    private BigDecimal buyTurnover;

    public void copy(InvestKlinesRecord source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
