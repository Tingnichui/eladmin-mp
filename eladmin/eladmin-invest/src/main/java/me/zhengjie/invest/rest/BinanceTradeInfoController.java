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
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.vo.BinanceTradeStatsInfoVO;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.invest.domain.vo.BinanceTradeInfoQueryCriteria;
import lombok.RequiredArgsConstructor;
import java.util.List;

import me.zhengjie.invest.task.SyncBinanceTradeInfoServiceTask;
import me.zhengjie.utils.StringUtils;
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
* @date 2025-07-05
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "币安交易管理")
@RequestMapping("/api/binanceTradeInfo")
public class BinanceTradeInfoController {

    private final BinanceTradeInfoService binanceTradeInfoService;
    private final SyncBinanceTradeInfoServiceTask syncBinanceTradeInfoServiceTask;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('binanceTradeInfo:list')")
    public void exportBinanceTradeInfo(HttpServletResponse response, BinanceTradeInfoQueryCriteria criteria) throws IOException {
        binanceTradeInfoService.download(binanceTradeInfoService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询币安交易")
    @ApiOperation("查询币安交易")
    @PreAuthorize("@el.check('binanceTradeInfo:list')")
    public ResponseEntity<PageResult<BinanceTradeInfo>> queryBinanceTradeInfo(BinanceTradeInfoQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(binanceTradeInfoService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增币安交易")
    @ApiOperation("新增币安交易")
    @PreAuthorize("@el.check('binanceTradeInfo:add')")
    public ResponseEntity<Object> createBinanceTradeInfo(@Validated @RequestBody BinanceTradeInfo resources){
        binanceTradeInfoService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改币安交易")
    @ApiOperation("修改币安交易")
    @PreAuthorize("@el.check('binanceTradeInfo:edit')")
    public ResponseEntity<Object> updateBinanceTradeInfo(@Validated @RequestBody BinanceTradeInfo resources){
        binanceTradeInfoService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除币安交易")
    @ApiOperation("删除币安交易")
    @PreAuthorize("@el.check('binanceTradeInfo:del')")
    public ResponseEntity<Object> deleteBinanceTradeInfo(@RequestBody List<Long> ids) {
        binanceTradeInfoService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/stats")
    @Log("查询交易汇总")
    @ApiOperation("查询交易汇总")
    @PreAuthorize("@el.check('binanceTradeInfo:list')")
    public ResponseEntity<BinanceTradeStatsInfoVO> queryBinanceTradeInfo(BinanceTradeInfoQueryCriteria criteria){
        return new ResponseEntity<>(binanceTradeInfoService.stats(criteria),HttpStatus.OK);
    }

    @PutMapping("/sync")
    @Log("同步交易")
    @ApiOperation("同步交易")
    @PreAuthorize("@el.check('binanceTradeInfo:sync')")
    public ResponseEntity<BinanceTradeStatsInfoVO> sync(){
        binanceTradeInfoService.syncAll();
        return new ResponseEntity<>(HttpStatus.OK);
    }

}