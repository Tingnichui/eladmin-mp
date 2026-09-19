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

import me.zhengjie.invest.domain.BinanceSpotCorePosition;
import me.zhengjie.invest.domain.dto.BinanceSpotCorePositionCandidate;
import me.zhengjie.invest.domain.dto.BinanceSpotCorePositionQueryCriteria;
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
public interface BinanceSpotCorePositionMapper extends BaseMapper<BinanceSpotCorePosition> {

    IPage<BinanceSpotCorePosition> findAll(@Param("criteria") BinanceSpotCorePositionQueryCriteria criteria, Page<Object> page);

    BinanceSpotCorePosition findActiveByTradeForUpdate(@Param("uid") Integer uid,
                                                        @Param("symbol") String symbol,
                                                        @Param("tradeId") Long tradeId);

    BinanceSpotCorePosition findByIdForUpdate(@Param("id") Long id);

    List<BinanceSpotCorePositionCandidate> findCandidates(@Param("uid") Integer uid,
                                                          @Param("symbol") String symbol);
}
