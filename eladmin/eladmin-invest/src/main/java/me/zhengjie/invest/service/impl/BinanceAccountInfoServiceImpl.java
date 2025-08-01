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

import me.zhengjie.exception.BadRequestException;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.utils.*;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.domain.vo.BinanceAccountInfoQueryCriteria;
import me.zhengjie.invest.mapper.BinanceAccountInfoMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
* @description 服务实现
* @author genghui
* @date 2025-08-02
**/
@Service
@RequiredArgsConstructor
public class BinanceAccountInfoServiceImpl extends ServiceImpl<BinanceAccountInfoMapper, BinanceAccountInfo> implements BinanceAccountInfoService {

    private final BinanceAccountInfoMapper binanceAccountInfoMapper;

    @Override
    public PageResult<BinanceAccountInfo> queryAll(BinanceAccountInfoQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(binanceAccountInfoMapper.findAll(criteria, page));
    }

    @Override
    public List<BinanceAccountInfo> queryAll(BinanceAccountInfoQueryCriteria criteria){
        return binanceAccountInfoMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(BinanceAccountInfo resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BinanceAccountInfo resources) {
        BinanceAccountInfo binanceAccountInfo = getById(resources.getId());
        binanceAccountInfo.copy(resources);
        saveOrUpdate(binanceAccountInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Integer> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<BinanceAccountInfo> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (BinanceAccountInfo binanceAccountInfo : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("身份证姓名", binanceAccountInfo.getIdCardName());
            map.put("用户编号", binanceAccountInfo.getUid());
            map.put("手机号，加密", binanceAccountInfo.getPhoneNumber());
            map.put("邮箱，加密", binanceAccountInfo.getEmail());
            map.put(" totalInvestment",  binanceAccountInfo.getTotalInvestment());
            map.put("apiKey，加密", binanceAccountInfo.getApiKey());
            map.put("apiSecret，加密", binanceAccountInfo.getApiSecret());
            map.put("备注", binanceAccountInfo.getRemark());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}