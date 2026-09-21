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
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceFuturesTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceFuturesTradeInfoQueryCriteria;
import me.zhengjie.invest.domain.dto.BinanceUsdFuturesStatsInfoVO;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceFuturesTradeInfoService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.utils.PageResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private final BinanceAccountInfoService binanceAccountInfoService;

    @GetMapping("/stats")
    @Log("查询U本位合约统计")
    @ApiOperation("查询U本位合约统计")
    @PreAuthorize("@el.check('binanceFuturesTradeInfo:list')")
    public ResponseEntity<BinanceUsdFuturesStatsInfoVO> queryStats(@RequestParam Integer uid,
                                                                   @RequestParam String symbol,
                                                                   @RequestParam String positionSide) {
        String normalizedSymbol = normalizeSymbol(symbol);
        String normalizedPositionSide = normalizePositionSide(positionSide);
        BinanceAccountInfo accountInfo = requireAvailableAccount(uid);
        BinanceUsdFuturesStatsInfoVO[] result = new BinanceUsdFuturesStatsInfoVO[1];
        BinanceAccountContextHolder.runWith(accountInfo,
                () -> result[0] = binanceFuturesTradeInfoService.queryStats(
                        uid, normalizedSymbol, normalizedPositionSide));
        return ResponseEntity.ok(result[0]);
    }

    @PutMapping("/syncSelected")
    @Log("同步当前账户U本位成交")
    @ApiOperation("同步当前账户U本位成交")
    @PreAuthorize("@el.check('binanceTradeInfo:sync')")
    public ResponseEntity<Map<String, Integer>> syncSelected(@RequestParam Integer uid,
                                                              @RequestParam String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        BinanceAccountInfo accountInfo = requireAvailableAccount(uid);
        int tradeCount = binanceFuturesTradeInfoService.sync(accountInfo, normalizedSymbol);
        return ResponseEntity.ok(Collections.singletonMap("tradeCount", tradeCount));
    }

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
    public ResponseEntity<PageResult<BinanceFuturesTradeInfo>> queryBinanceFuturesTradeInfo(BinanceFuturesTradeInfoQueryCriteria criteria){
        Page<Object> page = new Page<>(criteria.getPage(), criteria.getSize());
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

    private BinanceAccountInfo requireAvailableAccount(Integer uid) {
        BinanceAccountInfo accountInfo = binanceAccountInfoService.getAccountByUid(uid);
        if (!Integer.valueOf(1).equals(accountInfo.getApiValidFlag())) {
            throw new BadRequestException("当前账户 API 不可用");
        }
        return accountInfo;
    }

    private String normalizeSymbol(String symbol) {
        String normalized = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9]{5,20}")) {
            throw new BadRequestException("交易对格式不正确");
        }
        return normalized;
    }

    private String normalizePositionSide(String positionSide) {
        String normalized = positionSide == null ? "" : positionSide.trim().toUpperCase(Locale.ROOT);
        if (!"LONG".equals(normalized) && !"SHORT".equals(normalized) && !"BOTH".equals(normalized)) {
            throw new BadRequestException("持仓方向必须是 LONG、SHORT 或 BOTH");
        }
        return normalized;
    }
}
