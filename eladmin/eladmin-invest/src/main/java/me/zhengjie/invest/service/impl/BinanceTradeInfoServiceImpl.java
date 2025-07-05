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
package me.zhengjie.invest.service.impl;

import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.vo.BinanceTradeStatsInfoVO;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.invest.domain.vo.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.mapper.BinanceTradeInfoMapper;
import me.zhengjie.utils.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.zhengjie.utils.PageUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;

import me.zhengjie.utils.PageResult;

/**
* @description 服务实现
* @author genghui
* @date 2025-07-05
**/
@Service
@RequiredArgsConstructor
public class BinanceTradeInfoServiceImpl extends ServiceImpl<BinanceTradeInfoMapper, BinanceTradeInfo> implements BinanceTradeInfoService {

    private final BinanceTradeInfoMapper binanceTradeInfoMapper;

    @Override
    public PageResult<BinanceTradeInfo> queryAll(BinanceTradeInfoQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(binanceTradeInfoMapper.findAll(criteria, page));
    }

    @Override
    public List<BinanceTradeInfo> queryAll(BinanceTradeInfoQueryCriteria criteria){
        return binanceTradeInfoMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(BinanceTradeInfo resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BinanceTradeInfo resources) {
        BinanceTradeInfo binanceTradeInfo = getById(resources.getId());
        binanceTradeInfo.copy(resources);
        saveOrUpdate(binanceTradeInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<BinanceTradeInfo> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (BinanceTradeInfo binanceTradeInfo : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("交易对", binanceTradeInfo.getSymbol());
            map.put("成交价格", binanceTradeInfo.getPrice());
            map.put("成交数量", binanceTradeInfo.getQty());
            map.put("手续费", binanceTradeInfo.getCommission());
            map.put("成交时间", binanceTradeInfo.getTime());
            map.put("订单 ID", binanceTradeInfo.getOrderId());
            map.put("成交额", binanceTradeInfo.getQuoteQty());
            map.put("手续费资产", binanceTradeInfo.getCommissionAsset());
            map.put("是否为买方", binanceTradeInfo.getIsBuyer());
            map.put("是否为挂单方", binanceTradeInfo.getIsMaker());
            map.put("是否为最佳匹配", binanceTradeInfo.getIsBestMatch());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    @Value("${proxy.host}")
    private String proxyHost;
    @Value("${proxy.port}")
    private Integer proxyPort;
    @Value("${binance.api_host}")
    private String apiHost;
    @Value("${binance.api_key}")
    private String apiKey;
    @Value("${binance.api_secret}")
    private String apiSecret;

    @Override
    public void syncTradeInfo(String symbol) {
        String url = "/api/v3/myTrades";

        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
//        params.put("startTime", startTime.getTime());
        params.put("timestamp", System.currentTimeMillis());

        String queryString = params.entrySet().stream()
                .filter(entry -> StringUtils.isNotBlank(entry.getValue().toString()))
                .map(entry -> entry.getKey() + "=" + URLEncodeUtil.encode(entry.getValue().toString()))
                .collect(Collectors.joining("&"));

        String signature = new HMac(HmacAlgorithm.HmacSHA256, apiSecret.getBytes(StandardCharsets.UTF_8)).digestHex(queryString);
        params.put("signature", signature);

        String finalQuery = params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + URLEncodeUtil.encode(entry.getValue().toString()))
                .collect(Collectors.joining("&"));

        String fullUrl = apiHost + url + "?" + finalQuery;

        String body = HttpUtil.createGet(fullUrl)
                .header("X-MBX-APIKEY", apiKey)
                .setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)))
                .execute().body();

        // 打印响应结果
        System.out.println("Response: " + body);

        List<BinanceTradeInfo> javaList = JSON.parseArray(body).toJavaList(BinanceTradeInfo.class);
        System.err.println(javaList);

        // 查询已经在库中的数据
        List<Long> haveInDbOrderIdList = this.list(
                Wrappers.lambdaQuery(BinanceTradeInfo.class)
                        .select(BinanceTradeInfo::getOrderId)
                        .in(BinanceTradeInfo::getOrderId, javaList.stream().map(BinanceTradeInfo::getOrderId).collect(Collectors.toList()))
        ).stream().map(BinanceTradeInfo::getOrderId).collect(Collectors.toList());

        javaList.removeIf(v -> haveInDbOrderIdList.contains(v.getOrderId()));

        for (BinanceTradeInfo binanceTradeInfo : javaList) {
            this.saveOrUpdate(binanceTradeInfo);
        }

    }

    @Override
    public BinanceTradeStatsInfoVO stats(BinanceTradeInfoQueryCriteria criteria) {
        String symbol = criteria.getSymbol();
        // 查询所有买入 价格从低到高
        List<BinanceTradeInfo> buyTradeList = this.list(
                Wrappers.lambdaQuery(BinanceTradeInfo.class)
                        .eq(BinanceTradeInfo::getSymbol, symbol)
                        .eq(BinanceTradeInfo::getIsBuyer, 1)
                        .orderByAsc(BinanceTradeInfo::getPrice)
        );

        // 查询所有卖出 价格从高到低
        List<BinanceTradeInfo> sellTradeList = this.list(
                Wrappers.lambdaQuery(BinanceTradeInfo.class)
                        .eq(BinanceTradeInfo::getSymbol, symbol)
                        .eq(BinanceTradeInfo::getIsBuyer, 0)
                        .orderByDesc(BinanceTradeInfo::getPrice)
        );

        // 匹配高低 最高的卖出等量匹配最低的买入
        int buyIndex = 0;
        List<BinanceTradeInfo> matchedBuyList = new ArrayList<>();
        for (BinanceTradeInfo sell : sellTradeList) {
            BigDecimal sellQty = sell.getQty();

            while (sellQty.compareTo(BigDecimal.ZERO) > 0 && buyIndex < buyTradeList.size()) {
                BinanceTradeInfo buyTradeInfo = buyTradeList.get(buyIndex);
                // 已匹配完，跳过
                if (buyTradeInfo.getQty().compareTo(BigDecimal.ZERO) == 0) {
                    buyIndex++;
                    continue;
                }
                // 可匹配的仓位数量
                BigDecimal matchQty = sellQty.min(buyTradeInfo.getQty());

                // 拷贝买入记录用于展示匹配信息（只包含部分成交）
                BinanceTradeInfo partialBuy = new BinanceTradeInfo();
                BeanUtils.copyProperties(buyTradeInfo, partialBuy);
                partialBuy.setQty(matchQty);
                matchedBuyList.add(partialBuy);

                // 更新买入剩余量
                buyTradeInfo.setQty(buyTradeInfo.getQty().subtract(matchQty));
                // 更新卖出剩余量
                sellQty = sellQty.subtract(matchQty);

                // 买入剩余量不足
                if (buyTradeInfo.getQty().compareTo(BigDecimal.ZERO) == 0) {
                    buyIndex++;
                }
            }

        }

        BinanceTradeStatsInfoVO statsInfoVO = new BinanceTradeStatsInfoVO();

        // 买入总金额
        BigDecimal totalBuyAmount = matchedBuyList.stream()
                .map(b -> b.getQty().multiply(b.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statsInfoVO.setTotalBuyAmount(totalBuyAmount);
        // 买入总数量
        BigDecimal totalBuyQty = matchedBuyList.stream()
                .map(BinanceTradeInfo::getQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statsInfoVO.setTotalBuyQty(totalBuyQty);
        // 买入均价
        BigDecimal avgBuyPrice = totalBuyAmount.divide(totalBuyQty, 8, RoundingMode.HALF_UP);
        statsInfoVO.setAvgBuyPrice(avgBuyPrice);

        // 卖出总金额
        BigDecimal totalSellAmount = sellTradeList.stream()
                .map(b -> b.getQty().multiply(b.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statsInfoVO.setTotalSellAmount(totalSellAmount);
        // 卖出总数量
        BigDecimal totalSellQty = sellTradeList.stream()
                .map(BinanceTradeInfo::getQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statsInfoVO.setTotalSellQty(totalSellQty);
        // 卖出均价
        BigDecimal avgSellPrice = totalSellAmount.divide(totalSellQty, 8, RoundingMode.HALF_UP);
        statsInfoVO.setAvgSellPrice(avgSellPrice);

        // 总的利润
        BigDecimal profit = totalSellAmount.subtract(totalBuyAmount);
        statsInfoVO.setProfit(profit);
        // 收益率
        BigDecimal profitPct = profit.divide(totalBuyAmount, 4, RoundingMode.HALF_UP);
        statsInfoVO.setProfitPct(profitPct);

        // 剩余未平仓总金额
        BigDecimal totalWaitSellAmount = buyTradeList.stream()
                .map(b -> b.getQty().multiply(b.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statsInfoVO.setTotalWaitSellAmount(totalWaitSellAmount);
        // 剩余未平仓总数量
        BigDecimal totalWaitSellQty = buyTradeList.stream()
                .map(BinanceTradeInfo::getQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statsInfoVO.setTotalWaitSellQty(totalWaitSellQty);
        // 剩余未平仓均价
        BigDecimal totalWaitAvgSellPrice = totalWaitSellAmount.divide(totalWaitSellQty, 8, RoundingMode.HALF_UP);
        statsInfoVO.setTotalWaitAvgSellPrice(totalWaitAvgSellPrice);

        return statsInfoVO;

    }

}