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
import me.zhengjie.invest.domain.BinanceFuturesTradeInfo;
import me.zhengjie.invest.service.BinanceFuturesTradeInfoService;
import me.zhengjie.invest.domain.vo.BinanceFuturesTradeInfoQueryCriteria;
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
* @date 2025-10-17
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "币安合约交易信息管理")
@RequestMapping("/api/binanceFuturesTradeInfo")
public class BinanceFuturesTradeInfoController {

    private final BinanceFuturesTradeInfoService binanceFuturesTradeInfoService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('binanceFuturesTradeInfo:list')")
    public void exportBinanceFuturesTradeInfo(HttpServletResponse response, BinanceFuturesTradeInfoQueryCriteria criteria) throws IOException {
        binanceFuturesTradeInfoService.download(binanceFuturesTradeInfoService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询币安合约交易信息")
    @ApiOperation("查询币安合约交易信息")
    @PreAuthorize("@el.check('binanceFuturesTradeInfo:list')")
    public ResponseEntity<PageResult<BinanceFuturesTradeInfo>> queryBinanceFuturesTradeInfo(BinanceFuturesTradeInfoQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(binanceFuturesTradeInfoService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增币安合约交易信息")
    @ApiOperation("新增币安合约交易信息")
    @PreAuthorize("@el.check('binanceFuturesTradeInfo:add')")
    public ResponseEntity<Object> createBinanceFuturesTradeInfo(@Validated @RequestBody BinanceFuturesTradeInfo resources){
        binanceFuturesTradeInfoService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改币安合约交易信息")
    @ApiOperation("修改币安合约交易信息")
    @PreAuthorize("@el.check('binanceFuturesTradeInfo:edit')")
    public ResponseEntity<Object> updateBinanceFuturesTradeInfo(@Validated @RequestBody BinanceFuturesTradeInfo resources){
        binanceFuturesTradeInfoService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除币安合约交易信息")
    @ApiOperation("删除币安合约交易信息")
    @PreAuthorize("@el.check('binanceFuturesTradeInfo:del')")
    public ResponseEntity<Object> deleteBinanceFuturesTradeInfo(@RequestBody List<Long> ids) {
        binanceFuturesTradeInfoService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}