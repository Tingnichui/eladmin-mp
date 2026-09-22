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
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
* @description /
* @author genghui
* @date 2026-09-19
**/
@Data
@TableName("binance_spot_trade_match_state")
public class BinanceSpotTradeMatchState implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "主键")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @NotNull
    @ApiModelProperty(value = "币安成交 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tradeId;

    @NotNull
    @ApiModelProperty(value = "币安账户用户编号")
    private Integer uid;

    @NotBlank
    @ApiModelProperty(value = "现货交易对")
    private String symbol;

    @NotNull
    @ApiModelProperty(value = "是否为买方：1买入，0卖出")
    private Integer isBuyer;

    @NotNull
    @ApiModelProperty(value = "成交时间")
    private Timestamp tradeTime;

    @NotNull
    @ApiModelProperty(value = "原始成交数量")
    private BigDecimal originalQty;

    @NotNull
    @ApiModelProperty(value = "已撮合数量")
    private BigDecimal matchedQty;

    @NotNull
    @ApiModelProperty(value = "剩余未撮合数量")
    private BigDecimal remainingQty;

    @NotBlank
    @ApiModelProperty(value = "撮合状态：PENDING、PARTIAL、COMPLETED、EXCEPTION、SOURCE_EXCEPTION")
    private String matchStatus;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @TableField(exist = false)
    @ApiModelProperty(value = "成交价格")
    private BigDecimal price;

    @TableField(exist = false)
    @ApiModelProperty(value = "币安订单 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;

    @TableField(exist = false)
    @ApiModelProperty(value = "当前有效底仓数量")
    private BigDecimal activeCoreQty;

    @TableField(exist = false)
    @ApiModelProperty(value = "当前有效底仓记录 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long corePositionId;

    @TableField(exist = false)
    @ApiModelProperty(value = "设为底仓时间")
    private Timestamp coreLockedAt;

    public void copy(BinanceSpotTradeMatchState source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
