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

import me.zhengjie.invest.domain.BinanceSpotTradeMatch;
import me.zhengjie.invest.domain.dto.BinanceSpotTradeMatchQueryCriteria;
import me.zhengjie.invest.domain.dto.BinanceSpotTradeStatsAggregate;
import me.zhengjie.invest.domain.dto.SpotFeeAssetSummary;
import me.zhengjie.invest.domain.dto.SpotTradeMatchFeeRevaluation;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;

/**
* @author genghui
* @date 2026-09-19
**/
@Mapper
public interface BinanceSpotTradeMatchMapper extends BaseMapper<BinanceSpotTradeMatch> {

    IPage<BinanceSpotTradeMatch> findAll(@Param("criteria") BinanceSpotTradeMatchQueryCriteria criteria, Page<Object> page);

    BinanceSpotTradeStatsAggregate aggregateStats(@Param("uid") Integer uid,
                                                   @Param("symbol") String symbol);

    List<SpotFeeAssetSummary> aggregateFeeAssets(@Param("uid") Integer uid,
                                                  @Param("symbol") String symbol);

    List<SpotTradeMatchFeeRevaluation> findFeeRevaluationRows(@Param("uid") Integer uid,
                                                               @Param("symbol") String symbol);

    int updateFeeValuation(@Param("id") Long id,
                           @Param("buyFeeAsset") String buyFeeAsset,
                           @Param("buyFeeAmount") BigDecimal buyFeeAmount,
                           @Param("buyFeeQuoteAmount") BigDecimal buyFeeQuoteAmount,
                           @Param("sellFeeAsset") String sellFeeAsset,
                           @Param("sellFeeAmount") BigDecimal sellFeeAmount,
                           @Param("sellFeeQuoteAmount") BigDecimal sellFeeQuoteAmount,
                           @Param("feeValuationComplete") int feeValuationComplete,
                           @Param("fee") BigDecimal fee,
                           @Param("netPnl") BigDecimal netPnl);
}
