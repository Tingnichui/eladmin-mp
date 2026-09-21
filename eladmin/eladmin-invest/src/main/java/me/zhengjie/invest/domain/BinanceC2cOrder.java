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
* @date 2026-09-21
**/
@Data
@TableName("binance_c2c_order")
public class BinanceC2cOrder implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "主键")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @NotNull
    @ApiModelProperty(value = "币安账户用户编号")
    private Integer uid;

    @NotBlank
    @ApiModelProperty(value = "币安订单号（orderNumber）")
    private String orderNumber;

    @ApiModelProperty(value = "广告编号（advNo）")
    private String advNo;

    @NotBlank
    @ApiModelProperty(value = "交易方向（tradeType）：BUY、SELL")
    private String tradeType;

    @NotBlank
    @ApiModelProperty(value = "数字资产（asset），如 USDT")
    private String asset;

    @NotBlank
    @ApiModelProperty(value = "法币（fiat），如 CNY")
    private String fiat;

    @ApiModelProperty(value = "法币符号（fiatSymbol）")
    private String fiatSymbol;

    @ApiModelProperty(value = "数字资产数量（amount）")
    private BigDecimal amount;

    @ApiModelProperty(value = "数字资产数量（takerAmount）")
    private BigDecimal takerAmount;

    @NotNull
    @ApiModelProperty(value = "法币总金额（totalPrice）")
    private BigDecimal totalPrice;

    @ApiModelProperty(value = "成交单价（unitPrice）")
    private BigDecimal unitPrice;

    @NotBlank
    @ApiModelProperty(value = "订单状态（orderStatus）")
    private String orderStatus;

    @NotNull
    @ApiModelProperty(value = "币安订单创建时间戳（createTime，毫秒）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderCreateTime;

    @NotNull
    @ApiModelProperty(value = "币安订单创建时间（本地转换值）")
    private Timestamp orderTime;

    @ApiModelProperty(value = "手续费（commission）")
    private BigDecimal commission;

    @ApiModelProperty(value = "交易对手昵称（counterPartNickName）")
    private String counterPartNickName;

    @ApiModelProperty(value = "广告角色（advertisementRole）")
    private String advertisementRole;

    @ApiModelProperty(value = "币安接口原始 JSON")
    private String rawData;

    @ApiModelProperty(value = "最近同步时间")
    private Timestamp syncTime;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    public void copy(BinanceC2cOrder source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
