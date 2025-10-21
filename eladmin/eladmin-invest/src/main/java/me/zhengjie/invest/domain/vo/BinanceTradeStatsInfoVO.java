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
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author genghui
 * @description /
 * @date 2025-07-05
 **/
@Data
public class BinanceTradeStatsInfoVO implements Serializable {
    //买入总金额
    private BigDecimal totalBuyAmount;
    //买入均价
    private BigDecimal avgBuyPrice;
    //卖出总金额
    private BigDecimal totalSellAmount;
    //卖出均价
    private BigDecimal avgSellPrice;
    //总的利润
    private BigDecimal profit;
    //收益率
    private BigDecimal profitPct;
    //剩余未平仓总金额
    private BigDecimal totalWaitSellAmount;
    //剩余未平仓总数量
    private BigDecimal totalWaitSellQty;
    //剩余未平仓均价
    private BigDecimal totalWaitAvgSellPrice;
    private Long avgHoldTimeMs;
    private Long minHoldTimeMs;
    private Long maxHoldTimeMs;
    //手续费
    private BigDecimal fee = BigDecimal.ZERO;

    private List<MatchedTradeInfo> matchedTradeInfoList;

    // 剩余未平仓交易
    private List<BinanceTradeInfo> waitSellTradeInfoList;


}
