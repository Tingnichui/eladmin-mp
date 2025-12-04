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
public class BinanceTradeStatsInfoVO extends BinanceBaseStatsVO implements Serializable {
    //买入总金额
    private BigDecimal totalBuyAmount;
    //卖出总金额
    private BigDecimal totalSellAmount;
    //上一次净盈亏
    private BigDecimal lastNetPnl;
    //收益率
    private BigDecimal roi;
    // 持仓盈利
    private BigDecimal holdingProfit = BigDecimal.ZERO;
    // 持仓亏损
    private BigDecimal holdingLoss = BigDecimal.ZERO;
    // 持仓盈亏
    private BigDecimal holdingProfitLoss = BigDecimal.ZERO;
    // 当前现货价格
    private BigDecimal currentSpotPrice = BigDecimal.ZERO;

    // 剩余未平仓交易
    private List<BinanceTradeInfo> waitSellTradeInfoList;

    private List<MatchedTradeInfo> openTradeList;

    private BinanceFuturesTradeStatsInfoVO futuresTradeStatsInfo;

    private BinanceFuturesTradeStatsInfoVO coinFuturesTradeStatsInfo;

    private BinanceSpotHedgedTradeStatsInfoVO spotHedgedTradeStatsInfo;


}
