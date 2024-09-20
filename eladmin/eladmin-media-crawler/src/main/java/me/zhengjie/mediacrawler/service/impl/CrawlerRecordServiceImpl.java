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
import me.zhengjie.mediacrawler.domain.CrawlerRecord;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.mediacrawler.service.CrawlerRecordService;
import me.zhengjie.mediacrawler.domain.vo.CrawlerRecordQueryCriteria;
import me.zhengjie.mediacrawler.mapper.CrawlerRecordMapper;
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
* @date 2024-09-20
**/
@DS("media_crawler")
@Service
@RequiredArgsConstructor
public class CrawlerRecordServiceImpl extends ServiceImpl<CrawlerRecordMapper, CrawlerRecord> implements CrawlerRecordService {

    private final CrawlerRecordMapper crawlerRecordMapper;

    @Override
    public PageResult<CrawlerRecord> queryAll(CrawlerRecordQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(crawlerRecordMapper.findAll(criteria, page));
    }

    @Override
    public List<CrawlerRecord> queryAll(CrawlerRecordQueryCriteria criteria){
        return crawlerRecordMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(CrawlerRecord resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CrawlerRecord resources) {
        CrawlerRecord crawlerRecord = getById(resources.getId());
        crawlerRecord.copy(resources);
        saveOrUpdate(crawlerRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Integer> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<CrawlerRecord> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (CrawlerRecord crawlerRecord : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("自媒体平台", crawlerRecord.getPlatform());
            map.put("爬取类型", crawlerRecord.getCrawlerType());
            map.put("关键词", crawlerRecord.getKeywords());
            map.put("开始页数", crawlerRecord.getStartPage());
            map.put("结束页数", crawlerRecord.getEndPage());
            map.put("爬虫状态", crawlerRecord.getCrawlerStatus());
            map.put("异常信息", crawlerRecord.getErrorMsg());
            map.put("日志路径", crawlerRecord.getLogPath());
            map.put("开始时间", crawlerRecord.getStartTime());
            map.put("结束时间", crawlerRecord.getEndTime());
            map.put("创建者", crawlerRecord.getCreateBy());
            map.put("更新者", crawlerRecord.getUpdateBy());
            map.put("创建日期", crawlerRecord.getCreateTime());
            map.put("更新时间", crawlerRecord.getUpdateTime());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}