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
package me.zhengjie.mediacrawler.rest;

import me.zhengjie.annotation.Log;
import me.zhengjie.mediacrawler.domain.CrawlerRecord;
import me.zhengjie.mediacrawler.service.CrawlerRecordService;
import me.zhengjie.mediacrawler.domain.vo.CrawlerRecordQueryCriteria;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.annotations.*;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.zhengjie.utils.PageResult;

/**
* @author genghui
* @date 2024-09-20
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "爬虫记录管理")
@RequestMapping("/api/crawlerRecord")
public class CrawlerRecordController {

    private final CrawlerRecordService crawlerRecordService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('crawlerRecord:list')")
    public void exportCrawlerRecord(HttpServletResponse response, CrawlerRecordQueryCriteria criteria) throws IOException {
        crawlerRecordService.download(crawlerRecordService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询爬虫记录")
    @ApiOperation("查询爬虫记录")
    @PreAuthorize("@el.check('crawlerRecord:list')")
    public ResponseEntity<PageResult<CrawlerRecord>> queryCrawlerRecord(CrawlerRecordQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(crawlerRecordService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增爬虫记录")
    @ApiOperation("新增爬虫记录")
    @PreAuthorize("@el.check('crawlerRecord:add')")
    public ResponseEntity<Object> createCrawlerRecord(@Validated @RequestBody CrawlerRecord resources){
        crawlerRecordService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改爬虫记录")
    @ApiOperation("修改爬虫记录")
    @PreAuthorize("@el.check('crawlerRecord:edit')")
    public ResponseEntity<Object> updateCrawlerRecord(@Validated @RequestBody CrawlerRecord resources){
        crawlerRecordService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除爬虫记录")
    @ApiOperation("删除爬虫记录")
    @PreAuthorize("@el.check('crawlerRecord:del')")
    public ResponseEntity<Object> deleteCrawlerRecord(@RequestBody List<Integer> ids) {
        crawlerRecordService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}