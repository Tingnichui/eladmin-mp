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
package me.zhengjie.invest.domain.dto;

import lombok.Data;
import java.sql.Timestamp;
import java.util.List;
import io.swagger.annotations.ApiModelProperty;

/**
* @author genghui
* @date 2026-09-21
**/
@Data
public class BinanceC2cOrderQueryCriteria{

    @ApiModelProperty(value = "页码", example = "1")
    private Integer page = 1;

    @ApiModelProperty(value = "每页数据量", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "币安账户用户编号")
    private Integer uid;

    @ApiModelProperty(value = "币安订单号（orderNumber）")
    private String orderNumber;

    @ApiModelProperty(value = "交易方向（tradeType）：BUY、SELL")
    private String tradeType;

    @ApiModelProperty(value = "数字资产（asset），如 USDT")
    private String asset;

    @ApiModelProperty(value = "法币（fiat），如 CNY")
    private String fiat;

    @ApiModelProperty(value = "订单状态（orderStatus）")
    private String orderStatus;
    private List<Timestamp> orderTime;
    private List<Timestamp> syncTime;
}
