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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import me.zhengjie.annotation.Log;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.BinanceTradeInfoExt;
import me.zhengjie.invest.domain.vo.BinanceFuturesTradeStatsInfoVO;
import me.zhengjie.invest.domain.vo.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.domain.vo.BinanceTradeStatsInfoVO;
import me.zhengjie.invest.service.BinanceCoinFuturesTradeInfoService;
import me.zhengjie.invest.service.BinanceFuturesTradeInfoService;
import me.zhengjie.invest.service.BinanceTradeInfoExtService;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.utils.PageResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @author genghui
* @date 2025-07-05
**/
@RestController
@RequiredArgsConstructor
@Api(tags = "币安交易管理")
@RequestMapping("/api/binanceTradeInfo")
public class BinanceTradeInfoController {

    @Resource
    private BinanceTradeInfoService binanceTradeInfoService;
    @Resource
    private BinanceFuturesTradeInfoService binanceFuturesTradeInfoService;
    @Resource
    private BinanceCoinFuturesTradeInfoService binanceCoinFuturesTradeInfoService;
    @Resource
    private BinanceTradeInfoExtService binanceTradeInfoExtService;

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
    public ResponseEntity<Object> queryBinanceTradeInfo(BinanceTradeInfoQueryCriteria criteria){

        // 移除所有锁仓
        binanceTradeInfoExtService.getBaseMapper().update(null,
                Wrappers.lambdaUpdate(BinanceTradeInfoExt.class)
                        .set(BinanceTradeInfoExt::getHedgedQty, 0)
        );

        Map<String, Object> resMap = new HashMap<>();
        resMap.put("usdFuturesStatsInfo", binanceFuturesTradeInfoService.stats());
        resMap.put("coinFuturesStatsInfo", binanceCoinFuturesTradeInfoService.stats());
        resMap.put("spotFuturesStatsInfo", binanceTradeInfoService.stats(criteria));
        resMap.put("spotHedgedFuturesStatsInfo", binanceTradeInfoService.hedgedStats());

        return new ResponseEntity<>(resMap,HttpStatus.OK);
    }

    @PutMapping("/syncSpotTradeInfo")
    @Log("同步交易")
    @ApiOperation("同步交易")
    @PreAuthorize("@el.check('binanceTradeInfo:sync')")
    public ResponseEntity<BinanceTradeStatsInfoVO> sync(){
        binanceTradeInfoService.syncAll();
        binanceFuturesTradeInfoService.sync();
        binanceCoinFuturesTradeInfoService.sync();
        return new ResponseEntity<>(HttpStatus.OK);
    }

}