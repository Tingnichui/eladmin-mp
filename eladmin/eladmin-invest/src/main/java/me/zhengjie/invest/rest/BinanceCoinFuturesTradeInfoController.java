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
import me.zhengjie.invest.domain.BinanceCoinFuturesTradeInfo;
import me.zhengjie.invest.service.BinanceCoinFuturesTradeInfoService;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesTradeInfoQueryCriteria;
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
* @date 2025-11-23
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "币安-币本位合约交易信息管理")
@RequestMapping("/api/binanceCoinFuturesTradeInfo")
public class BinanceCoinFuturesTradeInfoController {

    private final BinanceCoinFuturesTradeInfoService binanceCoinFuturesTradeInfoService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('binanceCoinFuturesTradeInfo:list')")
    public void exportBinanceCoinFuturesTradeInfo(HttpServletResponse response, BinanceCoinFuturesTradeInfoQueryCriteria criteria) throws IOException {
        binanceCoinFuturesTradeInfoService.download(binanceCoinFuturesTradeInfoService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询币安-币本位合约交易信息")
    @ApiOperation("查询币安-币本位合约交易信息")
    @PreAuthorize("@el.check('binanceCoinFuturesTradeInfo:list')")
    public ResponseEntity<PageResult<BinanceCoinFuturesTradeInfo>> queryBinanceCoinFuturesTradeInfo(BinanceCoinFuturesTradeInfoQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(binanceCoinFuturesTradeInfoService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增币安-币本位合约交易信息")
    @ApiOperation("新增币安-币本位合约交易信息")
    @PreAuthorize("@el.check('binanceCoinFuturesTradeInfo:add')")
    public ResponseEntity<Object> createBinanceCoinFuturesTradeInfo(@Validated @RequestBody BinanceCoinFuturesTradeInfo resources){
        binanceCoinFuturesTradeInfoService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改币安-币本位合约交易信息")
    @ApiOperation("修改币安-币本位合约交易信息")
    @PreAuthorize("@el.check('binanceCoinFuturesTradeInfo:edit')")
    public ResponseEntity<Object> updateBinanceCoinFuturesTradeInfo(@Validated @RequestBody BinanceCoinFuturesTradeInfo resources){
        binanceCoinFuturesTradeInfoService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除币安-币本位合约交易信息")
    @ApiOperation("删除币安-币本位合约交易信息")
    @PreAuthorize("@el.check('binanceCoinFuturesTradeInfo:del')")
    public ResponseEntity<Object> deleteBinanceCoinFuturesTradeInfo(@RequestBody List<Long> ids) {
        binanceCoinFuturesTradeInfoService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}