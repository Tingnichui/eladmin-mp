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
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceCoinFuturesTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesStatsInfoVO;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesOrderDto;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesOrderRequest;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceCoinFuturesTradeInfoService;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesTradeInfoQueryCriteria;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
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
    private final BinanceAccountInfoService binanceAccountInfoService;

    @GetMapping("/stats")
    @Log("查询币本位合约统计")
    @ApiOperation("查询币本位合约统计")
    @PreAuthorize("@el.check('binanceCoinFuturesTradeInfo:list')")
    public ResponseEntity<BinanceCoinFuturesStatsInfoVO> queryStats(@RequestParam Integer uid,
                                                                    @RequestParam String symbol,
                                                                    @RequestParam String positionSide) {
        String normalizedSymbol = normalizeSymbol(symbol);
        String normalizedPositionSide = normalizePositionSide(positionSide);
        BinanceAccountInfo accountInfo = requireAvailableAccount(uid);
        BinanceCoinFuturesStatsInfoVO[] result = new BinanceCoinFuturesStatsInfoVO[1];
        BinanceAccountContextHolder.runWith(accountInfo,
                () -> result[0] = binanceCoinFuturesTradeInfoService.queryStats(
                        uid, normalizedSymbol, normalizedPositionSide));
        return ResponseEntity.ok(result[0]);
    }

    @PutMapping("/syncSelected")
    @Log("同步当前账户币本位成交")
    @ApiOperation("同步当前账户币本位成交")
    @PreAuthorize("@el.check('binanceTradeInfo:sync')")
    public ResponseEntity<Map<String, Integer>> syncSelected(@RequestParam Integer uid,
                                                              @RequestParam String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        BinanceAccountInfo accountInfo = requireAvailableAccount(uid);
        int tradeCount = binanceCoinFuturesTradeInfoService.sync(accountInfo, normalizedSymbol);
        return ResponseEntity.ok(Collections.singletonMap("tradeCount", tradeCount));
    }

    @PostMapping("/order")
    @Log("币本位合约下单")
    @ApiOperation("币本位合约市价或限价下单")
    @PreAuthorize("@el.check('binanceCoinFuturesTradeInfo:order')")
    public ResponseEntity<BinanceCoinFuturesOrderDto> placeOrder(
            @Validated @RequestBody BinanceCoinFuturesOrderRequest request) {
        request.setSymbol(normalizeSymbol(request.getSymbol()));
        BinanceAccountInfo accountInfo = requireAvailableAccount(request.getUid());
        BinanceCoinFuturesOrderDto[] result = new BinanceCoinFuturesOrderDto[1];
        BinanceAccountContextHolder.runWith(accountInfo,
                () -> result[0] = binanceCoinFuturesTradeInfoService.placeOrder(request));
        return ResponseEntity.ok(result[0]);
    }

    @GetMapping("/open-orders")
    @Log("查询币本位合约当前挂单")
    @ApiOperation("查询币本位合约当前挂单")
    @PreAuthorize("@el.check('binanceCoinFuturesTradeInfo:order')")
    public ResponseEntity<List<BinanceCoinFuturesOrderDto>> listOpenOrders(
            @RequestParam Integer uid, @RequestParam String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        BinanceAccountInfo accountInfo = requireAvailableAccount(uid);
        AtomicReference<List<BinanceCoinFuturesOrderDto>> result = new AtomicReference<>();
        BinanceAccountContextHolder.runWith(accountInfo,
                () -> result.set(binanceCoinFuturesTradeInfoService.listOpenOrders(normalizedSymbol)));
        return ResponseEntity.ok(result.get());
    }

    @GetMapping("/order")
    @Log("查询币本位合约订单")
    @ApiOperation("查询币本位合约订单")
    @PreAuthorize("@el.check('binanceCoinFuturesTradeInfo:order')")
    public ResponseEntity<BinanceCoinFuturesOrderDto> queryOrder(
            @RequestParam Integer uid, @RequestParam String symbol, @RequestParam Long orderId) {
        String normalizedSymbol = normalizeSymbol(symbol);
        BinanceAccountInfo accountInfo = requireAvailableAccount(uid);
        BinanceCoinFuturesOrderDto[] result = new BinanceCoinFuturesOrderDto[1];
        BinanceAccountContextHolder.runWith(accountInfo,
                () -> result[0] = binanceCoinFuturesTradeInfoService.queryOrder(normalizedSymbol, orderId));
        return ResponseEntity.ok(result[0]);
    }

    @DeleteMapping("/order")
    @Log("撤销币本位合约订单")
    @ApiOperation("撤销币本位合约订单")
    @PreAuthorize("@el.check('binanceCoinFuturesTradeInfo:order')")
    public ResponseEntity<BinanceCoinFuturesOrderDto> cancelOrder(
            @RequestParam Integer uid, @RequestParam String symbol, @RequestParam Long orderId) {
        String normalizedSymbol = normalizeSymbol(symbol);
        BinanceAccountInfo accountInfo = requireAvailableAccount(uid);
        BinanceCoinFuturesOrderDto[] result = new BinanceCoinFuturesOrderDto[1];
        BinanceAccountContextHolder.runWith(accountInfo,
                () -> result[0] = binanceCoinFuturesTradeInfoService.cancelOrder(normalizedSymbol, orderId));
        return ResponseEntity.ok(result[0]);
    }

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
    public ResponseEntity<PageResult<BinanceCoinFuturesTradeInfo>> queryBinanceCoinFuturesTradeInfo(BinanceCoinFuturesTradeInfoQueryCriteria criteria){
        Page<Object> page = new Page<>(criteria.getPage(), criteria.getSize());
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

    private BinanceAccountInfo requireAvailableAccount(Integer uid) {
        BinanceAccountInfo accountInfo = binanceAccountInfoService.getAccountByUid(uid);
        if (accountInfo == null || !Integer.valueOf(1).equals(accountInfo.getApiValidFlag())) {
            throw new BadRequestException("当前账户 API 不可用");
        }
        return accountInfo;
    }

    private String normalizeSymbol(String symbol) {
        String normalized = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
        if (!"BTCUSD_PERP".equals(normalized)) {
            throw new BadRequestException("当前仅支持 BTCUSD_PERP");
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
