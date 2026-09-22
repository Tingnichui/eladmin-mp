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
package me.zhengjie.invest.mapper;

import me.zhengjie.invest.domain.BinanceSpotTradeMatchState;
import me.zhengjie.invest.domain.dto.BinanceSpotTradeMatchStateQueryCriteria;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
* @author genghui
* @date 2026-09-19
**/
@Mapper
public interface BinanceSpotTradeMatchStateMapper extends BaseMapper<BinanceSpotTradeMatchState> {

    IPage<BinanceSpotTradeMatchState> findAll(@Param("criteria") BinanceSpotTradeMatchStateQueryCriteria criteria, Page<Object> page);

    int initializeFromTrades(@Param("uid") Integer uid, @Param("symbol") String symbol);

    BinanceSpotTradeMatchState findBuyStateForUpdate(@Param("uid") Integer uid,
                                                       @Param("symbol") String symbol,
                                                       @Param("tradeId") Long tradeId);

    List<BinanceSpotTradeMatchState> findPendingSellsForUpdate(@Param("uid") Integer uid,
                                                               @Param("symbol") String symbol);

    List<BinanceSpotTradeMatchState> findAvailableBuysForUpdate(@Param("uid") Integer uid,
                                                                @Param("symbol") String symbol,
                                                                @Param("sellTime") Timestamp sellTime,
                                                                @Param("sellTradeId") Long sellTradeId);

    List<BinanceSpotTradeMatchState> findAvailableBuysByTradeIdForUpdate(@Param("uid") Integer uid,
                                                                         @Param("symbol") String symbol,
                                                                         @Param("sourceTradeId") Long sourceTradeId,
                                                                         @Param("sellTime") Timestamp sellTime,
                                                                         @Param("sellTradeId") Long sellTradeId);

    List<BinanceSpotTradeMatchState> findAvailableBuysByOrderIdForUpdate(@Param("uid") Integer uid,
                                                                         @Param("symbol") String symbol,
                                                                         @Param("sourceOrderId") Long sourceOrderId,
                                                                         @Param("sellTime") Timestamp sellTime,
                                                                         @Param("sellTradeId") Long sellTradeId);

    int updateMatchProgress(@Param("id") Long id,
                            @Param("matchedQty") BigDecimal matchedQty,
                            @Param("remainingQty") BigDecimal remainingQty,
                            @Param("matchStatus") String matchStatus);

    List<BinanceSpotTradeMatchState> findStatsOpenBuys(@Param("uid") Integer uid,
                                                       @Param("symbol") String symbol);

    BigDecimal sumStatsUnmatchedSellQty(@Param("uid") Integer uid,
                                        @Param("symbol") String symbol);
}
