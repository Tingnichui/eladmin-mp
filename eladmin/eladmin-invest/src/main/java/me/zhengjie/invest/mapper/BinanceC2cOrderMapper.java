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

import me.zhengjie.invest.domain.BinanceC2cOrder;
import me.zhengjie.invest.domain.dto.BinanceC2cAccountAssetsVO;
import me.zhengjie.invest.domain.dto.BinanceC2cOrderQueryCriteria;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;

/**
* @author genghui
* @date 2026-09-21
**/
@Mapper
public interface BinanceC2cOrderMapper extends BaseMapper<BinanceC2cOrder> {

    IPage<BinanceC2cOrder> findAll(@Param("criteria") BinanceC2cOrderQueryCriteria criteria, Page<Object> page);

    int upsertBatch(@Param("orders") List<BinanceC2cOrder> orders);

    Long findLatestOrderCreateTime(@Param("uid") Integer uid);

    BinanceC2cAccountAssetsVO findAccountAssets(@Param("uid") Integer uid);
}
