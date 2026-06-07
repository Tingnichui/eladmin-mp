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
* @date 2026-06-07
**/
@Data
@TableName("invest_trade_analysis")
public class InvestTradeAnalysis implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "ID")
    private Long id;

    @NotBlank
    @ApiModelProperty(value = "方向")
    private String direction;

    @ApiModelProperty(value = "数量(USDT)")
    private BigDecimal amount;

    @ApiModelProperty(value = "入场类型")
    private String entryType;

    @ApiModelProperty(value = "开仓时间")
    private Timestamp openTime;

    @ApiModelProperty(value = "平仓时间")
    private Timestamp closeTime;

    @ApiModelProperty(value = "开仓价")
    private BigDecimal openPrice;

    @ApiModelProperty(value = "平仓价")
    private BigDecimal closePrice;

    @ApiModelProperty(value = "净盈亏")
    private BigDecimal netProfit;

    @ApiModelProperty(value = "开仓原因")
    private String openReason;

    @ApiModelProperty(value = "开仓K线图")
    private String openKlineImages;

    @ApiModelProperty(value = "平仓K线图")
    private String closeKlineImages;

    @ApiModelProperty(value = "开仓评分")
    private Integer score;

    @ApiModelProperty(value = "复盘结论")
    private String reviewConclusion;

    @ApiModelProperty(value = "交易质量")
    private Integer qualityLevel;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建者")
    private String createBy;

    @ApiModelProperty(value = "更新者")
    private String updateBy;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    public void copy(InvestTradeAnalysis source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
