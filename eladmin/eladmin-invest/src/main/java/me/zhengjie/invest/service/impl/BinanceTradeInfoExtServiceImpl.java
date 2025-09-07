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
package me.zhengjie.invest.service.impl;

import me.zhengjie.invest.domain.BinanceTradeInfoExt;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.BinanceTradeInfoExtService;
import me.zhengjie.invest.domain.vo.BinanceTradeInfoExtQueryCriteria;
import me.zhengjie.invest.mapper.BinanceTradeInfoExtMapper;
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
* @date 2025-09-07
**/
@Service
@RequiredArgsConstructor
public class BinanceTradeInfoExtServiceImpl extends ServiceImpl<BinanceTradeInfoExtMapper, BinanceTradeInfoExt> implements BinanceTradeInfoExtService {

    private final BinanceTradeInfoExtMapper binanceTradeInfoExtMapper;

    @Override
    public PageResult<BinanceTradeInfoExt> queryAll(BinanceTradeInfoExtQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(binanceTradeInfoExtMapper.findAll(criteria, page));
    }

    @Override
    public List<BinanceTradeInfoExt> queryAll(BinanceTradeInfoExtQueryCriteria criteria){
        return binanceTradeInfoExtMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(BinanceTradeInfoExt resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BinanceTradeInfoExt resources) {
        BinanceTradeInfoExt binanceTradeInfoExt = getById(resources.getId());
        binanceTradeInfoExt.copy(resources);
        saveOrUpdate(binanceTradeInfoExt);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Integer> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<BinanceTradeInfoExt> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (BinanceTradeInfoExt binanceTradeInfoExt : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("订单 ID", binanceTradeInfoExt.getOrderId());
            map.put("仓位编号", binanceTradeInfoExt.getPosId());
            map.put("是否锁仓；0未锁仓 1锁仓", binanceTradeInfoExt.getHedgedFlag());
            map.put("备注", binanceTradeInfoExt.getRemark());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}