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
package me.zhengjie.invest.rest;

import me.zhengjie.annotation.Log;
import me.zhengjie.invest.domain.InvestTradeAnalysis;
import me.zhengjie.invest.service.InvestTradeAnalysisService;
import me.zhengjie.invest.domain.vo.InvestTradeAnalysisQueryCriteria;
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
* @date 2026-06-07
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "交易分析管理")
@RequestMapping("/api/investTradeAnalysis")
public class InvestTradeAnalysisController {

    private final InvestTradeAnalysisService investTradeAnalysisService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('investTradeAnalysis:list')")
    public void exportInvestTradeAnalysis(HttpServletResponse response, InvestTradeAnalysisQueryCriteria criteria) throws IOException {
        investTradeAnalysisService.download(investTradeAnalysisService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询交易分析")
    @ApiOperation("查询交易分析")
    @PreAuthorize("@el.check('investTradeAnalysis:list')")
    public ResponseEntity<PageResult<InvestTradeAnalysis>> queryInvestTradeAnalysis(InvestTradeAnalysisQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(investTradeAnalysisService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增交易分析")
    @ApiOperation("新增交易分析")
    @PreAuthorize("@el.check('investTradeAnalysis:add')")
    public ResponseEntity<Object> createInvestTradeAnalysis(@Validated @RequestBody InvestTradeAnalysis resources){
        investTradeAnalysisService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改交易分析")
    @ApiOperation("修改交易分析")
    @PreAuthorize("@el.check('investTradeAnalysis:edit')")
    public ResponseEntity<Object> updateInvestTradeAnalysis(@Validated @RequestBody InvestTradeAnalysis resources){
        investTradeAnalysisService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除交易分析")
    @ApiOperation("删除交易分析")
    @PreAuthorize("@el.check('investTradeAnalysis:del')")
    public ResponseEntity<Object> deleteInvestTradeAnalysis(@RequestBody List<Long> ids) {
        investTradeAnalysisService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}