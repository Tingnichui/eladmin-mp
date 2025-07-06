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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.invest.constants.TradePairingLogicEnum;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;
import me.zhengjie.invest.domain.vo.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.domain.vo.BinanceTradeStatsInfoVO;
import me.zhengjie.invest.mapper.BinanceTradeInfoMapper;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.utils.FileUtil;
import me.zhengjie.utils.PageResult;
import me.zhengjie.utils.PageUtil;
import me.zhengjie.utils.StringUtils;
import me.zhengjie.utils.enums.OrderDirectionEnum;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author genghui
 * @description 服务实现
 * @date 2025-07-05
 **/
@Service
@RequiredArgsConstructor
public class BinanceTradeInfoServiceImpl extends ServiceImpl<BinanceTradeInfoMapper, BinanceTradeInfo> implements BinanceTradeInfoService {

    private final BinanceTradeInfoMapper binanceTradeInfoMapper;

    @Override
    public PageResult<BinanceTradeInfo> queryAll(BinanceTradeInfoQueryCriteria criteria, Page<Object> page) {
        return PageUtil.toPage(binanceTradeInfoMapper.findAll(criteria, page));
    }

    @Override
    public List<BinanceTradeInfo> queryAll(BinanceTradeInfoQueryCriteria criteria) {
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
            Map<String, Object> map = new LinkedHashMap<>();
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
        List<BinanceTradeInfo> buyTradeList, sellTradeList;

        TradePairingLogicEnum tradePairingLogicEnum = TradePairingLogicEnum.getByKey(criteria.getTradePairingLogic());
        switch (tradePairingLogicEnum) {
            // 最大收益
            case MAX_PROFIT:
                // 查询所有买入 价格从低到高
                criteria.setOrderColumn("price");
                criteria.setOrderDirection(OrderDirectionEnum.ASC.getValue());
                criteria.setIsBuyer(1);
                buyTradeList = binanceTradeInfoMapper.findAll(criteria);
                // 查询所有卖出 价格从高到低
                criteria.setOrderColumn("price");
                criteria.setOrderDirection(OrderDirectionEnum.DESC.getValue());
                criteria.setIsBuyer(0);
                sellTradeList = binanceTradeInfoMapper.findAll(criteria);
                break;
            // 时间顺序 先进先出
            case FIFO:
                // 查询所有买入 时间从早到晚
                criteria.setOrderColumn("time");
                criteria.setOrderDirection(OrderDirectionEnum.ASC.getValue());
                criteria.setIsBuyer(1);
                buyTradeList = binanceTradeInfoMapper.findAll(criteria);
                // 查询所有卖出 时间从早到晚
                criteria.setOrderColumn("time");
                criteria.setOrderDirection(OrderDirectionEnum.ASC.getValue());
                criteria.setIsBuyer(0);
                sellTradeList = binanceTradeInfoMapper.findAll(criteria);
                break;
            default:
                throw new BadRequestException("未知撮合方式");
        }


        // 撮合交易对
        List<MatchedTradeInfo> matchedList = new ArrayList<>();
        Iterator<BinanceTradeInfo> buyIterator = buyTradeList.iterator();
        while (buyIterator.hasNext()) {
            BinanceTradeInfo buy = buyIterator.next();
            // 按照顺序寻找卖单
            Iterator<BinanceTradeInfo> sellIterator = sellTradeList.iterator();
            while (sellIterator.hasNext()) {
                // 进入撮合首先判断是否买入还有剩余 已经平仓掉了就直接去除
                if (buy.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                    buyIterator.remove();
                    break;
                }

                BinanceTradeInfo sell = sellIterator.next();
                // 可匹配的仓位数量
                BigDecimal matchQty = buy.getQty().min(sell.getQty());
                // 撮合交易记录
                MatchedTradeInfo matched = new MatchedTradeInfo();
                matched.setQty(matchQty);
                matched.setBuyPrice(buy.getPrice());
                matched.setSellPrice(sell.getPrice());
                matched.setBuyTime(buy.getTime());
                matched.setSellTime(sell.getTime());
                // 是否符合撮合策略
                if (!tradePairingLogicEnum.allowMatch(matched)) {
                    continue;
                }

                matchedList.add(matched);
                // 更新买入剩余量
                buy.setQty(buy.getQty().subtract(matchQty));
                // 更新卖出剩余量
                sell.setQty(sell.getQty().subtract(matchQty));

                // 卖出是否还有剩余 没有剩余就直接移除
                if (sell.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                    sellIterator.remove();
                    continue;
                }

            }

        }

        BinanceTradeStatsInfoVO statsInfoVO = new BinanceTradeStatsInfoVO();

        // 撮合总量
        BigDecimal totalQty = matchedList.stream()
                .map(MatchedTradeInfo::getQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 买入总金额
        BigDecimal totalBuyAmount = matchedList.stream()
                .map(MatchedTradeInfo::getBuyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statsInfoVO.setTotalBuyAmount(totalBuyAmount);
        // 买入均价
        BigDecimal avgBuyPrice = totalBuyAmount.divide(totalQty, 8, RoundingMode.HALF_UP);
        statsInfoVO.setAvgBuyPrice(avgBuyPrice);

        // 卖出总金额
        BigDecimal totalSellAmount = matchedList.stream()
                .map(MatchedTradeInfo::getSellAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statsInfoVO.setTotalSellAmount(totalSellAmount);
        // 卖出均价
        BigDecimal avgSellPrice = totalSellAmount.divide(totalQty, 8, RoundingMode.HALF_UP);
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

        // 持仓时间（单位：毫秒）
        List<Long> holdDurations = matchedList.stream()
                .map(m -> m.getSellTime().getTime() - m.getBuyTime().getTime())
                .collect(Collectors.toList());

        if (!holdDurations.isEmpty()) {
            // 平均持仓时间（毫秒）
            long avgHoldTimeMs = (long) holdDurations.stream().mapToLong(Long::longValue).average().orElse(0);
            // 最小/最大持仓时间
            long minHoldTimeMs = Collections.min(holdDurations);
            long maxHoldTimeMs = Collections.max(holdDurations);

            // 可选择换算成小时/分钟/秒
            statsInfoVO.setAvgHoldTimeMs(avgHoldTimeMs);
            statsInfoVO.setMinHoldTimeMs(minHoldTimeMs);
            statsInfoVO.setMaxHoldTimeMs(maxHoldTimeMs);
        }

        List<Map<String, Object>> profitVsHoldScatter = matchedList.stream()
                .map(m -> {
                    Map<String, Object> point = new HashMap<>();

                    // 持仓时间（小时，保留 2 位小数）
                    long durationMillis = m.getSellTime().getTime() - m.getBuyTime().getTime();
                    BigDecimal holdTimeHours = new BigDecimal(durationMillis)
                            .divide(BigDecimal.valueOf(3600_000), 2, RoundingMode.HALF_UP);
                    point.put("holdHours", holdTimeHours);

                    // 收益率（例如 0.0123 表示 1.23%）
                    point.put("profitRate", m.getProfitRate());

                    return point;
                })
                .collect(Collectors.toList());
        statsInfoVO.setProfitVsHoldScatter(profitVsHoldScatter);

        return statsInfoVO;

    }

}