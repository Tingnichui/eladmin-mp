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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import me.zhengjie.annotation.Log;
import me.zhengjie.invest.domain.BinanceTradeInfoExt;
import me.zhengjie.invest.domain.vo.BinanceTradeInfoExtQueryCriteria;
import me.zhengjie.invest.service.BinanceTradeInfoExtService;
import me.zhengjie.utils.PageResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
* @author genghui
* @date 2025-09-07
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "币安交易信息扩展管理")
@RequestMapping("/api/binanceTradeInfoExt")
public class BinanceTradeInfoExtController {

    private final BinanceTradeInfoExtService binanceTradeInfoExtService;

    @Log("导出数据")
    @ApiOperation("导出数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('binanceTradeInfoExt:list')")
    public void exportBinanceTradeInfoExt(HttpServletResponse response, BinanceTradeInfoExtQueryCriteria criteria) throws IOException {
        binanceTradeInfoExtService.download(binanceTradeInfoExtService.queryAll(criteria), response);
    }

    @GetMapping
    @Log("查询币安交易信息扩展")
    @ApiOperation("查询币安交易信息扩展")
    @PreAuthorize("@el.check('binanceTradeInfoExt:list')")
    public ResponseEntity<PageResult<BinanceTradeInfoExt>> queryBinanceTradeInfoExt(BinanceTradeInfoExtQueryCriteria criteria, Page<Object> page){
        return new ResponseEntity<>(binanceTradeInfoExtService.queryAll(criteria,page),HttpStatus.OK);
    }

    @PostMapping
    @Log("新增币安交易信息扩展")
    @ApiOperation("新增币安交易信息扩展")
    @PreAuthorize("@el.check('binanceTradeInfoExt:add')")
    public ResponseEntity<Object> createBinanceTradeInfoExt(@Validated @RequestBody BinanceTradeInfoExt resources){
        binanceTradeInfoExtService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    @Log("修改币安交易信息扩展")
    @ApiOperation("修改币安交易信息扩展")
    @PreAuthorize("@el.check('binanceTradeInfoExt:edit')")
    public ResponseEntity<Object> updateBinanceTradeInfoExt(@Validated @RequestBody BinanceTradeInfoExt resources){
        binanceTradeInfoExtService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping
    @Log("删除币安交易信息扩展")
    @ApiOperation("删除币安交易信息扩展")
    @PreAuthorize("@el.check('binanceTradeInfoExt:del')")
    public ResponseEntity<Object> deleteBinanceTradeInfoExt(@RequestBody List<Integer> ids) {
        binanceTradeInfoExtService.deleteAll(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/changeHedgedFlag")
    @Log("修改币安账户")
    @ApiOperation("修改币安账户")
    @PreAuthorize("@el.check('binanceTradeInfoExt:edit')")
    public ResponseEntity<Object> changeHedgedFlag(@RequestBody Long orderId){
//        binanceTradeInfoExtService.changeHedgedFlag(orderId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}