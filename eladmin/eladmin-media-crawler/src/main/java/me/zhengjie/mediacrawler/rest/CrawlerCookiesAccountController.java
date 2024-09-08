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
import me.zhengjie.mediacrawler.domain.CrawlerCookiesAccount;
import me.zhengjie.mediacrawler.service.CrawlerCookiesAccountService;
import me.zhengjie.mediacrawler.domain.vo.CrawlerCookiesAccountQueryCriteria;
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
* @date 2024-09-08
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "自媒体账号管理")
@RequestMapping("/api/crawlerCookiesAccount")
public class CrawlerCookiesAccountController {

    private final CrawlerCookiesAccountService crawlerCookiesAccountService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('crawlerCookiesAccount:list')")
    public void exportCrawlerCookiesAccount(HttpServletResponse response, CrawlerCookiesAccountQueryCriteria criteria) throws IOException {
        crawlerCookiesAccountService.download(crawlerCookiesAccountService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询自媒体账号")
    @ApiOperation("查询自媒体账号")
    @PreAuthorize("@el.check('crawlerCookiesAccount:list')")
    public ResponseEntity<PageResult<CrawlerCookiesAccount>> queryCrawlerCookiesAccount(CrawlerCookiesAccountQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(crawlerCookiesAccountService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增自媒体账号")
    @ApiOperation("新增自媒体账号")
    @PreAuthorize("@el.check('crawlerCookiesAccount:add')")
    public ResponseEntity<Object> createCrawlerCookiesAccount(@Validated @RequestBody CrawlerCookiesAccount resources){
        crawlerCookiesAccountService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改自媒体账号")
    @ApiOperation("修改自媒体账号")
    @PreAuthorize("@el.check('crawlerCookiesAccount:edit')")
    public ResponseEntity<Object> updateCrawlerCookiesAccount(@Validated @RequestBody CrawlerCookiesAccount resources){
        crawlerCookiesAccountService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除自媒体账号")
    @ApiOperation("删除自媒体账号")
    @PreAuthorize("@el.check('crawlerCookiesAccount:del')")
    public ResponseEntity<Object> deleteCrawlerCookiesAccount(@RequestBody List<Long> ids) {
        crawlerCookiesAccountService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}