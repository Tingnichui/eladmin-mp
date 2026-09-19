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
@TableName("binance_spot_core_position")
public class BinanceSpotCorePosition implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "主键")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @NotNull
    @ApiModelProperty(value = "币安账户用户编号")
    private Integer uid;

    @NotBlank
    @ApiModelProperty(value = "现货交易对，如 BTCUSDT")
    private String symbol;

    @NotNull
    @ApiModelProperty(value = "原始现货买入成交 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tradeId;

    @NotNull
    @ApiModelProperty(value = "锁定为底仓的数量")
    private BigDecimal coreQty;

    @NotNull
    @ApiModelProperty(value = "设为底仓时间")
    private Timestamp lockedAt;

    @ApiModelProperty(value = "解除底仓时间，空表示仍在锁定")
    private Timestamp releasedAt;

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

    public void copy(BinanceSpotCorePosition source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
