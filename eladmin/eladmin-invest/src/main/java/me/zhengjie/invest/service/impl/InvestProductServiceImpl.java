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

import me.zhengjie.invest.domain.InvestProduct;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.InvestProductService;
import me.zhengjie.invest.domain.dto.InvestProductQueryCriteria;
import me.zhengjie.invest.mapper.InvestProductMapper;
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
* @date 2025-01-04
**/
@Service
@RequiredArgsConstructor
public class InvestProductServiceImpl extends ServiceImpl<InvestProductMapper, InvestProduct> implements InvestProductService {

    private final InvestProductMapper investProductMapper;

    @Override
    public PageResult<InvestProduct> queryAll(InvestProductQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(investProductMapper.findAll(criteria, page));
    }

    @Override
    public List<InvestProduct> queryAll(InvestProductQueryCriteria criteria){
        return investProductMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(InvestProduct resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(InvestProduct resources) {
        InvestProduct investProduct = getById(resources.getId());
        investProduct.copy(resources);
        saveOrUpdate(investProduct);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Integer> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<InvestProduct> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (InvestProduct investProduct : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("投资类型；1股票；2期货；3加密货币", investProduct.getInvestType());
            map.put("投资产品编码", investProduct.getInvestProductCode());
            map.put("投资产品名称", investProduct.getInvestProductName());
            map.put("最小购买单位；例如最小购买0.001，那么这边设置为1000", investProduct.getMinSize());
            map.put("计量单位", investProduct.getMeasurementUnit());
            map.put("创建时间", investProduct.getCreateTime());
            map.put("更新人", investProduct.getUpdateBy());
            map.put("更新时间", investProduct.getUpdateTime());
            map.put("创建人", investProduct.getCreateBy());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}