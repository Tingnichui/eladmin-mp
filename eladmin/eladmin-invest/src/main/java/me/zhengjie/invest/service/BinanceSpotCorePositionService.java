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
package me.zhengjie.invest.service;

import me.zhengjie.invest.domain.BinanceSpotCorePosition;
import me.zhengjie.invest.domain.dto.BinanceSpotCorePositionQueryCriteria;
import me.zhengjie.invest.domain.dto.BinanceSpotCorePositionAdjustRequest;
import me.zhengjie.invest.domain.dto.BinanceSpotCorePositionCandidate;
import me.zhengjie.invest.domain.dto.BinanceSpotCorePositionLockRequest;
import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import me.zhengjie.utils.PageResult;

/**
* @description 服务接口
* @author genghui
* @date 2026-09-19
**/
public interface BinanceSpotCorePositionService extends IService<BinanceSpotCorePosition> {

    /**
    * 查询数据分页
    * @param criteria 条件
    * @param page 分页参数
    * @return PageResult
    */
    PageResult<BinanceSpotCorePosition> queryAll(BinanceSpotCorePositionQueryCriteria criteria, Page<Object> page);

    /**
    * 创建
    * @param resources /
    */
    void create(BinanceSpotCorePosition resources);

    /**
    * 编辑
    * @param resources /
    */
    void update(BinanceSpotCorePosition resources);

    /**
    * 多选删除
    * @param ids /
    */
    void deleteAll(List<Long> ids);

    BinanceSpotCorePosition lock(BinanceSpotCorePositionLockRequest request);

    BinanceSpotCorePosition adjust(Long id, BinanceSpotCorePositionAdjustRequest request);

    BinanceSpotCorePosition release(Long id);

    List<BinanceSpotCorePosition> releaseAll(List<Long> ids);

    List<BinanceSpotCorePositionCandidate> listCandidates(Integer uid, String symbol);

    /**
    * 导出数据
    * @param criteria 条件参数
    * @param response /
    * @throws IOException /
    */
    void download(BinanceSpotCorePositionQueryCriteria criteria, HttpServletResponse response) throws IOException;
}
