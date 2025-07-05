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
package me.zhengjie.invest.domain.vo;

import lombok.Data;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.List;

/**
* @author genghui
* @date 2025-07-05
**/
@Data
public class BinanceTradeInfoQueryCriteria{
    private String symbol;
    private Long orderId;
    private String commissionAsset;
    private Integer isBuyer;
    private Integer isMaker;
    private Integer isBestMatch;
    private List<BigDecimal> price;
    private List<BigDecimal> qty;
    private List<BigDecimal> commission;
    private List<Timestamp> time;
    private List<BigDecimal> quoteQty;
}