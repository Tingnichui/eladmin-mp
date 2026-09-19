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
package me.zhengjie.invest.service.impl;

import me.zhengjie.invest.domain.BinanceSpotCorePosition;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.BinanceSpotCorePositionService;
import me.zhengjie.invest.domain.dto.BinanceSpotCorePositionQueryCriteria;
import me.zhengjie.invest.mapper.BinanceSpotCorePositionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.zhengjie.utils.PageUtil;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import me.zhengjie.utils.PageResult;

/**
* @description 服务实现
* @author genghui
* @date 2026-09-19
**/
@Service
@RequiredArgsConstructor
public class BinanceSpotCorePositionServiceImpl extends ServiceImpl<BinanceSpotCorePositionMapper, BinanceSpotCorePosition> implements BinanceSpotCorePositionService {

    private final BinanceSpotCorePositionMapper binanceSpotCorePositionMapper;

    @Override
    public PageResult<BinanceSpotCorePosition> queryAll(BinanceSpotCorePositionQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(binanceSpotCorePositionMapper.findAll(criteria, page));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(BinanceSpotCorePosition resources) {
        binanceSpotCorePositionMapper.insert(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BinanceSpotCorePosition resources) {
        BinanceSpotCorePosition binanceSpotCorePosition = getById(resources.getId());
        binanceSpotCorePosition.copy(resources);
        binanceSpotCorePositionMapper.updateById(binanceSpotCorePosition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        binanceSpotCorePositionMapper.deleteBatchIds(ids);
    }

    @Override
    public void download(BinanceSpotCorePositionQueryCriteria criteria, HttpServletResponse response) throws IOException {
        List<String> headers = new ArrayList<>();
        headers.add("币安账户用户编号");
        headers.add("现货交易对，如 BTCUSDT");
        headers.add("原始现货买入成交 ID");
        headers.add("锁定为底仓的数量");
        headers.add("设为底仓时间");
        headers.add("解除底仓时间，空表示仍在锁定");
        headers.add("备注");
        headers.add("创建者");
        headers.add("更新者");
        headers.add("创建时间");
        headers.add("更新时间");
        FileUtil.downloadExcel(headers, response, writer -> {
            long current = 1L;
            final long pageSize = 10000L;
            while (true) {
                Page<Object> page = new Page<>(current, pageSize, false);
                List<BinanceSpotCorePosition> records = binanceSpotCorePositionMapper.findAll(criteria, page).getRecords();
                if (records.isEmpty()) {
                    break;
                }
                List<Map<String, Object>> list = new ArrayList<>(records.size());
                for (BinanceSpotCorePosition binanceSpotCorePosition : records) {
                    Map<String,Object> map = new LinkedHashMap<>();
                    map.put("币安账户用户编号", binanceSpotCorePosition.getUid());
                    map.put("现货交易对，如 BTCUSDT", binanceSpotCorePosition.getSymbol());
                    map.put("原始现货买入成交 ID", getExportValue(binanceSpotCorePosition.getTradeId()));
                    map.put("锁定为底仓的数量", binanceSpotCorePosition.getCoreQty());
                    map.put("设为底仓时间", binanceSpotCorePosition.getLockedAt());
                    map.put("解除底仓时间，空表示仍在锁定", binanceSpotCorePosition.getReleasedAt());
                    map.put("备注", binanceSpotCorePosition.getRemark());
                    map.put("创建者", binanceSpotCorePosition.getCreateBy());
                    map.put("更新者", binanceSpotCorePosition.getUpdateBy());
                    map.put("创建时间", binanceSpotCorePosition.getCreateTime());
                    map.put("更新时间", binanceSpotCorePosition.getUpdateTime());
                    list.add(map);
                }
                writer.write(list);
                if (records.size() < pageSize) {
                    break;
                }
                current++;
            }
        });
    }

    private Object getExportValue(Object value) {
        return value instanceof Long ? String.valueOf(value) : value;
    }
}
