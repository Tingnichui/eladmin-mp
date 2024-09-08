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
package me.zhengjie.mediacrawler.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import me.zhengjie.mediacrawler.domain.CrawlerCookiesAccount;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.mediacrawler.service.CrawlerCookiesAccountService;
import me.zhengjie.mediacrawler.domain.vo.CrawlerCookiesAccountQueryCriteria;
import me.zhengjie.mediacrawler.mapper.CrawlerCookiesAccountMapper;
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
* @date 2024-09-08
**/
@DS("media_crawler")
@Service
@RequiredArgsConstructor
public class CrawlerCookiesAccountServiceImpl extends ServiceImpl<CrawlerCookiesAccountMapper, CrawlerCookiesAccount> implements CrawlerCookiesAccountService {

    private final CrawlerCookiesAccountMapper crawlerCookiesAccountMapper;

    @Override
    public PageResult<CrawlerCookiesAccount> queryAll(CrawlerCookiesAccountQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(crawlerCookiesAccountMapper.findAll(criteria, page));
    }

    @Override
    public List<CrawlerCookiesAccount> queryAll(CrawlerCookiesAccountQueryCriteria criteria){
        return crawlerCookiesAccountMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(CrawlerCookiesAccount resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CrawlerCookiesAccount resources) {
        CrawlerCookiesAccount crawlerCookiesAccount = getById(resources.getId());
        crawlerCookiesAccount.copy(resources);
        saveOrUpdate(crawlerCookiesAccount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<CrawlerCookiesAccount> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (CrawlerCookiesAccount crawlerCookiesAccount : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("账号名称", crawlerCookiesAccount.getAccountName());
            map.put("平台名称", crawlerCookiesAccount.getPlatformName());
            map.put("cookies", crawlerCookiesAccount.getCookies());
            map.put("创建时间", crawlerCookiesAccount.getCreateTime());
            map.put("更新时间", crawlerCookiesAccount.getUpdateTime());
            map.put("失效时", crawlerCookiesAccount.getInvalidTimestamp());
            map.put("状态", crawlerCookiesAccount.getStatus());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}