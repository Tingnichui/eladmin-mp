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
* @date 2026-09-29
**/
@Data
@TableName("research_strategy_alert_events")
public class ResearchStrategyAlertEvents implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @NotBlank
    @ApiModelProperty(value = "事件唯一键")
    private String eventKey;

    @NotNull
    @ApiModelProperty(value = "告警ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long alertId;

    @NotNull
    @ApiModelProperty(value = "结构版本")
    private Integer schemaVersion;

    @NotBlank
    @ApiModelProperty(value = "策略ID")
    private String strategyId;

    @NotBlank
    @ApiModelProperty(value = "交易对")
    private String symbol;

    @NotBlank
    @ApiModelProperty(value = "周期")
    private String intervalCode;

    @NotNull
    @ApiModelProperty(value = "策略版本")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long profileRevision;

    @NotBlank
    @ApiModelProperty(value = "参数指纹")
    private String parameterFingerprint;

    @NotBlank
    @ApiModelProperty(value = "数据源指纹")
    private String sourceFingerprint;

    @NotNull
    @ApiModelProperty(value = "K线开盘时间")
    private Timestamp barOpenTime;

    @NotNull
    @ApiModelProperty(value = "决策时间")
    private Timestamp decisionTime;

    @NotBlank
    @ApiModelProperty(value = "操作")
    private String action;

    @NotNull
    @ApiModelProperty(value = "操作前仓位")
    private Integer positionBefore;

    @NotNull
    @ApiModelProperty(value = "操作后仓位")
    private Integer positionAfter;

    @NotNull
    @ApiModelProperty(value = "操作价格")
    private BigDecimal actionPrice;

    @NotNull
    @ApiModelProperty(value = "收盘价格")
    private BigDecimal closePrice;

    @ApiModelProperty(value = "止损价格")
    private BigDecimal stopLossPrice;

    @NotBlank
    @ApiModelProperty(value = "触发原因")
    private String reason;

    @NotBlank
    @ApiModelProperty(value = "标题")
    private String title;

    @NotBlank
    @ApiModelProperty(value = "消息")
    private String message;

    @NotBlank
    @ApiModelProperty(value = "事件内容")
    private String payload;

    @NotNull
    @ApiModelProperty(value = "创建时间")
    private Timestamp createdAt;

    public void copy(ResearchStrategyAlertEvents source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
