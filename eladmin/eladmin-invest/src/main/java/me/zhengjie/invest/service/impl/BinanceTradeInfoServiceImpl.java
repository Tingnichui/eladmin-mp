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

import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceOrderApiDto;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;
import me.zhengjie.invest.domain.vo.BinanceOrderVO;
import me.zhengjie.invest.domain.vo.BinanceSpotHedgedTradeStatsInfoVO;
import me.zhengjie.invest.domain.vo.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.domain.vo.BinanceTradeStatsInfoVO;
import me.zhengjie.invest.mapper.BinanceTradeInfoMapper;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceTradeInfoExtService;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.invest.util.BinanceUsdFuturesUtil;
import me.zhengjie.invest.util.TradeMatcherUtil;
import me.zhengjie.utils.FileUtil;
import me.zhengjie.utils.PageResult;
import me.zhengjie.utils.PageUtil;
import me.zhengjie.utils.RedisUtils;
import me.zhengjie.utils.enums.OrderDirectionEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Resource
    private BinanceTradeInfoMapper binanceTradeInfoMapper;
    @Resource
    private BinanceAccountInfoService binanceAccountInfoService;
    @Resource
    private BinanceSpotUtil binanceSpotUtil;
    @Resource
    private BinanceTradeInfoExtService binanceTradeInfoExtService;
    @Resource
    private BinanceUsdFuturesUtil binanceUsdFuturesUtil;
    @Resource
    private RedisUtils redisUtils;

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

    @Override
    public void syncTradeInfo(String symbol) {
        // 查询所有账号
        List<BinanceAccountInfo> accountInfoList = binanceAccountInfoService.listUseApiAccount();
        for (BinanceAccountInfo accountInfo : accountInfoList) {
            // 设置账号信息
            BinanceAccountContextHolder.set(accountInfo);
            // 调用接口获取最近的订单信息
            List<BinanceTradeInfo> orderInfoList = binanceSpotUtil.getMyTrades(symbol);
            // 查询已经在库中的订单
            Set<Long> existIdSet = this.list(
                    Wrappers.lambdaQuery(BinanceTradeInfo.class)
                            .select(BinanceTradeInfo::getId)
                            .in(BinanceTradeInfo::getId, orderInfoList.stream().map(BinanceTradeInfo::getId).collect(Collectors.toSet()))
            ).stream().map(BinanceTradeInfo::getId).collect(Collectors.toSet());

            // 过滤掉已存在的订单
            List<BinanceTradeInfo> newOrders = orderInfoList.stream()
                    .filter(order -> !existIdSet.contains(order.getId()))
                    .peek(order -> order.setUid(accountInfo.getUid()))
                    .collect(Collectors.toList());

            // 保存新订单
            this.saveOrUpdateBatch(newOrders);

            // 清除账号信息
            BinanceAccountContextHolder.clear();
        }

    }

    @Override
    public BinanceTradeStatsInfoVO stats(BinanceTradeInfoQueryCriteria criteria) {
        final String key = "SPOT_LAST_NET_PNL";
        BinanceTradeStatsInfoVO statsInfoVO = new BinanceTradeStatsInfoVO();
        statsInfoVO.setLastNetPnl((BigDecimal) redisUtils.get(key));

        // 未锁仓的撮合交易
        criteria.setHedgedFlag(0);

        // 撮合交易对
        List<BinanceTradeInfo> openList, closeList;
        final boolean side = true;
        final String feeRate = "0.001";

        {
            // 查询所有买入 价格从低到高
            criteria.setOrderColumn("price");
            criteria.setOrderDirection(OrderDirectionEnum.ASC.getValue());
            criteria.setIsBuyer(1);
            openList = binanceTradeInfoMapper.findAll(criteria);
            // 查询所有卖出 时间从早到晚
            criteria.setOrderColumn("time");
            criteria.setOrderDirection(OrderDirectionEnum.ASC.getValue());
            criteria.setIsBuyer(0);
            closeList = binanceTradeInfoMapper.findAll(criteria);

            List<MatchedTradeInfo> matchedList = TradeMatcherUtil.matchTrades(
                    side,
                    feeRate,
                    openList,
                    closeList,
                    BinanceTradeInfo::getQty,
                    BinanceTradeInfo::setQty,
                    BinanceTradeInfo::getPrice,
                    BinanceTradeInfo::getTime,
                    matched -> {
                        return matched.getNetPnl().compareTo(criteria.getMinProfitPct()) >= 0;
                    }
            );

            // 买入总金额
            statsInfoVO.setTotalBuyAmount(matchedList.stream().map(MatchedTradeInfo::getOpenAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 卖出总金额
            statsInfoVO.setTotalSellAmount(matchedList.stream().map(MatchedTradeInfo::getCloseAmount).reduce(BigDecimal.ZERO, BigDecimal::add));

            // 盈亏
            statsInfoVO.setPnl(matchedList.stream().map(MatchedTradeInfo::getPnl).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 手续费
            statsInfoVO.setFee(matchedList.stream().map(MatchedTradeInfo::getFee).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 净盈亏
            statsInfoVO.setNetPnl(matchedList.stream().map(MatchedTradeInfo::getNetPnl).reduce(BigDecimal.ZERO, BigDecimal::add));
            redisUtils.set(key, statsInfoVO.getNetPnl());

        }



        // 剩余未平仓总金额
        BigDecimal totalWaitSellAmount = openList.stream().map(b -> b.getQty().multiply(b.getPrice())).reduce(BigDecimal.ZERO, BigDecimal::add);
        statsInfoVO.setPosAmount(totalWaitSellAmount);
        // 剩余未平仓总数量
        BigDecimal totalWaitSellQty = openList.stream()
                .map(BinanceTradeInfo::getQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statsInfoVO.setPosQty(totalWaitSellQty);
        // 剩余未平仓均价
        BigDecimal totalWaitAvgSellPrice = totalWaitSellAmount.divide(totalWaitSellQty, 8, RoundingMode.HALF_UP);
        statsInfoVO.setPosAvgPrice(totalWaitAvgSellPrice);

        // 剩余待平仓交易
        openList.sort(Comparator.comparing(BinanceTradeInfo::getPrice).reversed());

        try {
            BigDecimal currentPrice = binanceSpotUtil.getPrice(BinanceEnum.SYMBOL.valueOf(criteria.getSymbol()));
            statsInfoVO.setCurrentSpotPrice(currentPrice);


            List<MatchedTradeInfo> matchedTradeInfos = TradeMatcherUtil.matchTrades(side, feeRate, openList, BinanceTradeInfo::getQty, BinanceTradeInfo::getPrice, currentPrice, null);
            // 持仓盈利
            statsInfoVO.setHoldingProfit(matchedTradeInfos.stream().map(MatchedTradeInfo::getNetPnl).filter(netPnl -> netPnl.compareTo(BigDecimal.ZERO) > 0).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 持仓亏损
            statsInfoVO.setHoldingLoss(matchedTradeInfos.stream().map(MatchedTradeInfo::getNetPnl).filter(netPnl -> netPnl.compareTo(BigDecimal.ZERO) <= 0).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 持仓盈亏
            statsInfoVO.setHoldingProfitLoss(matchedTradeInfos.stream().map(MatchedTradeInfo::getNetPnl).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 持仓订单
            statsInfoVO.setTradeList(
                    matchedTradeInfos.stream().collect(Collectors.groupingBy(MatchedTradeInfo::getOpenPrice))
                            .entrySet()
                            .stream().map(v -> {
                                MatchedTradeInfo m = new MatchedTradeInfo(side, feeRate);
                                m.setOpenPrice(v.getKey());
                                m.setClosePrice(currentPrice);
                                m.setQty(v.getValue().stream().map(MatchedTradeInfo::getQty).reduce(BigDecimal.ZERO, BigDecimal::add));
                                return m;
                            }).collect(Collectors.toList())
                            .stream().sorted(Comparator.comparing(MatchedTradeInfo::getRoi).reversed())
                            .collect(Collectors.toList())
            );

        } catch (Exception e) {
        }

        return statsInfoVO;
    }

    @Override
    public void syncAll() {
        for (BinanceEnum.SYMBOL symbol : BinanceEnum.SYMBOL.values()) {
            if (symbol.getType() == 0) {
                this.syncTradeInfo(symbol.toString());
            }
        }

    }

    @Override
    public List<BinanceTradeInfo> list4hedge(BigDecimal lowPrice, BigDecimal highPrice, BigDecimal qty, Integer limit) {
        List<BinanceTradeInfo> binanceTradeInfos = binanceTradeInfoMapper.list4hedge(lowPrice, highPrice, qty, limit);
        if (null != qty) {
            BigDecimal netQty = binanceTradeInfos.stream().map(BinanceTradeInfo::getNetQty).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (netQty.compareTo(qty) < 0) {
                binanceTradeInfos = binanceTradeInfoMapper.list4hedge(lowPrice, null, qty, limit);
            }
        }
        return binanceTradeInfos;
    }

    @Override
    public BinanceSpotHedgedTradeStatsInfoVO hedgedStats() {
        BinanceSpotHedgedTradeStatsInfoVO statsInfo = new BinanceSpotHedgedTradeStatsInfoVO();

        BinanceTradeInfoQueryCriteria criteria = new BinanceTradeInfoQueryCriteria();
        criteria.setHedgedFlag(1);
        List<BinanceTradeInfo> hedgedTradeInfo = this.queryAll(criteria);
        if (CollectionUtils.isNotEmpty(hedgedTradeInfo)) {
            // 锁仓总额
            statsInfo.setPosAmount(hedgedTradeInfo.stream().map(BinanceTradeInfo::getHedgedAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 锁仓数量
            statsInfo.setPosQty(hedgedTradeInfo.stream().map(BinanceTradeInfo::getHedgedQty).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 锁仓均价
            statsInfo.setPosAvgPrice(NumberUtil.div(statsInfo.getPosAmount(), statsInfo.getPosQty()));
        }

        return statsInfo;
    }

    @Override
    public void createPos(BinanceOrderVO posInfo) {
        // 现货请求体
        BinanceOrderApiDto apiDto = new BinanceOrderApiDto();
        apiDto.setSymbol(BinanceEnum.SYMBOL.BTCUSDT.name());
        apiDto.setQuantity(posInfo.getPosQty());
        apiDto.setStopPrice(posInfo.getOpenPrice());

        // 做多
        if (posInfo.getPosDir()) {
            // 现货做多
            apiDto.setType(BinanceEnum.TYPE.STOP_LOSS);
            apiDto.setSide(BinanceEnum.SIDE.BUY);
            Long openOrderId = binanceSpotUtil.order(apiDto, 3);
            System.err.println(openOrderId);
            // 合约止损

        }
        // 做空
        else {
            // 合约做空
            // 现货止损

        }
    }

}