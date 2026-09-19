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

import me.zhengjie.invest.domain.BinanceSpotTradeMatchState;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.BinanceSpotTradeMatchStateService;
import me.zhengjie.invest.domain.dto.BinanceSpotTradeMatchStateQueryCriteria;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchStateMapper;
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
public class BinanceSpotTradeMatchStateServiceImpl extends ServiceImpl<BinanceSpotTradeMatchStateMapper, BinanceSpotTradeMatchState> implements BinanceSpotTradeMatchStateService {

    private final BinanceSpotTradeMatchStateMapper binanceSpotTradeMatchStateMapper;

    @Override
    public PageResult<BinanceSpotTradeMatchState> queryAll(BinanceSpotTradeMatchStateQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(binanceSpotTradeMatchStateMapper.findAll(criteria, page));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(BinanceSpotTradeMatchState resources) {
        binanceSpotTradeMatchStateMapper.insert(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BinanceSpotTradeMatchState resources) {
        BinanceSpotTradeMatchState binanceSpotTradeMatchState = getById(resources.getId());
        binanceSpotTradeMatchState.copy(resources);
        binanceSpotTradeMatchStateMapper.updateById(binanceSpotTradeMatchState);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        binanceSpotTradeMatchStateMapper.deleteBatchIds(ids);
    }

    @Override
    public void download(BinanceSpotTradeMatchStateQueryCriteria criteria, HttpServletResponse response) throws IOException {
        List<String> headers = new ArrayList<>();
        headers.add("币安成交 ID");
        headers.add("币安账户用户编号");
        headers.add("现货交易对");
        headers.add("是否为买方：1买入，0卖出");
        headers.add("成交时间");
        headers.add("原始成交数量");
        headers.add("已撮合数量");
        headers.add("剩余未撮合数量");
        headers.add("撮合状态：PENDING、PARTIAL、COMPLETED、EXCEPTION");
        headers.add("创建时间");
        headers.add("更新时间");
        FileUtil.downloadExcel(headers, response, writer -> {
            long current = 1L;
            final long pageSize = 10000L;
            while (true) {
                Page<Object> page = new Page<>(current, pageSize, false);
                List<BinanceSpotTradeMatchState> records = binanceSpotTradeMatchStateMapper.findAll(criteria, page).getRecords();
                if (records.isEmpty()) {
                    break;
                }
                List<Map<String, Object>> list = new ArrayList<>(records.size());
                for (BinanceSpotTradeMatchState binanceSpotTradeMatchState : records) {
                    Map<String,Object> map = new LinkedHashMap<>();
                    map.put("币安成交 ID", getExportValue(binanceSpotTradeMatchState.getTradeId()));
                    map.put("币安账户用户编号", binanceSpotTradeMatchState.getUid());
                    map.put("现货交易对", binanceSpotTradeMatchState.getSymbol());
                    map.put("是否为买方：1买入，0卖出", binanceSpotTradeMatchState.getIsBuyer());
                    map.put("成交时间", binanceSpotTradeMatchState.getTradeTime());
                    map.put("原始成交数量", binanceSpotTradeMatchState.getOriginalQty());
                    map.put("已撮合数量", binanceSpotTradeMatchState.getMatchedQty());
                    map.put("剩余未撮合数量", binanceSpotTradeMatchState.getRemainingQty());
                    map.put("撮合状态：PENDING、PARTIAL、COMPLETED、EXCEPTION", binanceSpotTradeMatchState.getMatchStatus());
                    map.put("创建时间", binanceSpotTradeMatchState.getCreateTime());
                    map.put("更新时间", binanceSpotTradeMatchState.getUpdateTime());
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
