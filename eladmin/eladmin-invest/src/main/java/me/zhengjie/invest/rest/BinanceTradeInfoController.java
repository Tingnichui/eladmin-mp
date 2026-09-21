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
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.annotation.Log;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.invest.domain.dto.BinanceOrderApiDto;
import me.zhengjie.invest.domain.dto.BinanceFuturesTradeStatsInfoVO;
import me.zhengjie.invest.domain.dto.BinanceOrderVO;
import me.zhengjie.invest.domain.dto.BinanceSpotTradeMatchResult;
import me.zhengjie.invest.domain.dto.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.domain.dto.BinanceTradeStatsInfoVO;
import me.zhengjie.invest.service.*;
import me.zhengjie.invest.service.support.BinanceStatsRealtimeService;
import me.zhengjie.invest.service.support.BinanceStatsRealtimeSnapshot;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.utils.PageResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @author genghui
* @date 2025-07-05
**/
@RestController
@RequiredArgsConstructor
@Slf4j
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
    private BinanceAccountInfoService binanceAccountInfoService;
    @Resource
    private BinanceStatsRealtimeService binanceStatsRealtimeService;
    @Resource
    private BinanceSpotTradeMatcherService binanceSpotTradeMatcherService;
    @Resource
    private BinanceC2cOrderService binanceC2cOrderService;

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
    public ResponseEntity<PageResult<BinanceTradeInfo>> queryBinanceTradeInfo(BinanceTradeInfoQueryCriteria criteria){
        Page<Object> page = new Page<>(criteria.getPage(), criteria.getSize());
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
    public ResponseEntity<Object> queryBinanceTradeStats(BinanceTradeInfoQueryCriteria criteria){
        if (criteria.getUid() == null) {
            throw new BadRequestException("请选择账户");
        }
        if (criteria.getSymbol() == null || criteria.getSymbol().trim().isEmpty()) {
            throw new BadRequestException("请选择交易对");
        }

        Map<String, Object> resMap = new HashMap<>();
        BinanceAccountInfo accountInfo = binanceAccountInfoService.getAccountByUid(criteria.getUid());
        BinanceAccountContextHolder.runWith(accountInfo, () -> {
            BinanceStatsRealtimeSnapshot realtimeSnapshot = binanceStatsRealtimeService.loadSpot(
                    criteria.getUid(), criteria.getSymbol());
            BinanceTradeStatsInfoVO spotStats = binanceTradeInfoService.stats(
                    criteria, realtimeSnapshot.getCurrentSpotPrice());
            resMap.put("spotFuturesStatsInfo", spotStats);
            resMap.put("realtimeStatus", realtimeSnapshot.getStatuses());
            List<String> warnings = new ArrayList<>(realtimeSnapshot.getWarnings());
            warnings.addAll(spotStats.getWarnings());
            resMap.put("warnings", warnings);
        });

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
        List<BinanceAccountInfo> accountInfoList = binanceAccountInfoService.listUseApiAccount();
        for (BinanceAccountInfo accountInfo : accountInfoList) {
            binanceC2cOrderService.sync(accountInfo.getUid());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/syncSelected")
    @Log("同步当前账户交易")
    @ApiOperation("同步当前账户交易")
    @PreAuthorize("@el.check('binanceTradeInfo:sync')")
    public ResponseEntity<Map<String, Object>> syncSelected(@RequestParam Integer uid,
                                                            @RequestParam String symbol) {
        BinanceEnum.SYMBOL spotSymbol;
        try {
            spotSymbol = BinanceEnum.SYMBOL.valueOf(symbol);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("交易对不存在");
        }
        if (!Integer.valueOf(0).equals(spotSymbol.getType())) {
            throw new BadRequestException("请选择现货交易对");
        }

        BinanceAccountInfo accountInfo = binanceAccountInfoService.getAccountByUid(uid);
        if (!Integer.valueOf(1).equals(accountInfo.getApiValidFlag())) {
            throw new BadRequestException("当前账户 API 不可用");
        }

        Map<String, Object> result = new HashMap<>();
        try {
            result.put("spotCount", binanceTradeInfoService.syncTradeInfo(accountInfo, spotSymbol.name()));
        } catch (Exception e) {
            log.warn("当前账户现货同步失败: uid={}, symbol={}, error={}", uid, symbol,
                    e.getClass().getSimpleName());
            throw new BadRequestException("现货同步失败，请稍后重试");
        }
        try {
            result.put("usdFuturesCount", binanceFuturesTradeInfoService.sync(accountInfo));
        } catch (Exception e) {
            log.warn("当前账户 U 本位同步失败: uid={}, error={}", uid, e.getClass().getSimpleName());
            throw new BadRequestException("U 本位同步失败，请稍后重试");
        }
        try {
            result.put("coinFuturesCount", binanceCoinFuturesTradeInfoService.sync(accountInfo));
        } catch (Exception e) {
            log.warn("当前账户币本位同步失败: uid={}, error={}", uid, e.getClass().getSimpleName());
            throw new BadRequestException("币本位同步失败，请稍后重试");
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PutMapping("/matchSpotTradeInfo")
    @Log("执行现货 FIFO 撮合")
    @ApiOperation("执行现货 FIFO 撮合")
    @PreAuthorize("@el.check('binanceTradeInfo:sync')")
    public ResponseEntity<BinanceSpotTradeMatchResult> matchSpotTradeInfo(@RequestParam Integer uid,
                                                                          @RequestParam String symbol) {
        BinanceEnum.SYMBOL spotSymbol;
        try {
            spotSymbol = BinanceEnum.SYMBOL.valueOf(symbol);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("交易对不存在");
        }
        if (!Integer.valueOf(0).equals(spotSymbol.getType())) {
            throw new BadRequestException("请选择现货交易对");
        }
        return ResponseEntity.ok(binanceSpotTradeMatcherService.initializeAndMatch(uid, spotSymbol.name()));
    }


    @PostMapping("/createPos")
    @Log("建仓")
    @PreAuthorize("@el.check('binanceTradeInfo:createPos')")
    public ResponseEntity<Object> createPos(@Validated @RequestBody BinanceOrderVO posInfo){
        binanceTradeInfoService.createPos(posInfo);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

}
