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
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceOrderApiDto;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceOrderVO;
import me.zhengjie.invest.domain.dto.BinanceSpotHedgedTradeStatsInfoVO;
import me.zhengjie.invest.domain.dto.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.domain.dto.BinanceTradeStatsInfoVO;
import me.zhengjie.invest.mapper.BinanceTradeInfoMapper;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceTradeInfoExtService;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.invest.service.support.BinanceSpotHedgeContext;
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
import org.springframework.beans.BeanUtils;
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
@Slf4j
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
        return stats(criteria, null);
    }

    @Override
    public BinanceTradeStatsInfoVO stats(BinanceTradeInfoQueryCriteria criteria, BinanceSpotHedgeContext hedgeContext) {
        return stats(criteria, hedgeContext, null);
    }

    @Override
    public BinanceTradeStatsInfoVO stats(BinanceTradeInfoQueryCriteria criteria,
                                         BinanceSpotHedgeContext hedgeContext,
                                         List<BinanceTradeInfo> snapshotTrades) {
        final String key = "SPOT_LAST_NET_PNL:" + criteria.getUid() + ":" + criteria.getSymbol();
        BinanceTradeStatsInfoVO statsInfoVO = new BinanceTradeStatsInfoVO();
        statsInfoVO.setLastNetPnl((BigDecimal) redisUtils.get(key));
        BinanceTradeInfoQueryCriteria statsCriteria = copyCriteria(criteria);

        // 未锁仓的撮合交易
        statsCriteria.setHedgedFlag(hedgeContext == null ? 0 : null);

        // 撮合交易对
        List<BinanceTradeInfo> openList, closeList;
        final boolean side = true;
        final String feeRate = "0.001";

        {
            // 查询所有买入 价格从低到高
            if (snapshotTrades == null) {
                statsCriteria.setOrderColumn("price");
                statsCriteria.setOrderDirection(OrderDirectionEnum.ASC.getValue());
                statsCriteria.setIsBuyer(1);
                openList = sanitizeTrades(binanceTradeInfoMapper.findAll(statsCriteria), statsInfoVO, "买入");
            } else {
                openList = snapshotTrades.stream()
                        .filter(trade -> Integer.valueOf(1).equals(trade.getIsBuyer()))
                        .map(this::copyTrade)
                        .sorted(Comparator.comparing(BinanceTradeInfo::getPrice))
                        .collect(Collectors.toCollection(ArrayList::new));
            }
            if (hedgeContext != null) {
                openList.forEach(trade -> trade.setQty(
                        trade.getQty().subtract(hedgeContext.getHedgedQty(trade.getId()))
                ));
                openList.removeIf(trade -> trade.getQty().compareTo(BigDecimal.ZERO) <= 0);
            }
            // 查询所有卖出 时间从早到晚
            if (snapshotTrades == null) {
                statsCriteria.setOrderColumn("time");
                statsCriteria.setOrderDirection(OrderDirectionEnum.ASC.getValue());
                statsCriteria.setIsBuyer(0);
                closeList = sanitizeTrades(binanceTradeInfoMapper.findAll(statsCriteria), statsInfoVO, "卖出");
            } else {
                closeList = snapshotTrades.stream()
                        .filter(trade -> Integer.valueOf(0).equals(trade.getIsBuyer()))
                        .map(this::copyTrade)
                        .sorted(Comparator.comparing(BinanceTradeInfo::getTime))
                        .collect(Collectors.toCollection(ArrayList::new));
            }

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
                        return matched.getNetPnl().compareTo(statsCriteria.getMinProfitPct()) >= 0;
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
        statsInfoVO.setPosAvgPrice(totalWaitSellQty.compareTo(BigDecimal.ZERO) > 0
                ? totalWaitSellAmount.divide(totalWaitSellQty, 8, RoundingMode.HALF_UP)
                : null);

        // 剩余待平仓交易
        openList.sort(Comparator.comparing(BinanceTradeInfo::getPrice).reversed());

        if (openList.isEmpty()) {
            return statsInfoVO;
        }

        try {
            BigDecimal currentPrice = binanceSpotUtil.getPrice(BinanceEnum.SYMBOL.valueOf(statsCriteria.getSymbol()));
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("现货价格为空或无效");
            }
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
            statsInfoVO.setCurrentSpotPrice(null);
            statsInfoVO.setTradeList(Collections.emptyList());
            addWarning(statsInfoVO, "现货持仓实时估值暂不可用");
            log.warn("现货持仓实时估值失败: uid={}, symbol={}, error={}",
                    statsCriteria.getUid(), statsCriteria.getSymbol(), e.getClass().getSimpleName());
        }

        return statsInfoVO;
    }

    private BinanceTradeInfoQueryCriteria copyCriteria(BinanceTradeInfoQueryCriteria source) {
        BinanceTradeInfoQueryCriteria target = new BinanceTradeInfoQueryCriteria();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private BinanceTradeInfo copyTrade(BinanceTradeInfo source) {
        BinanceTradeInfo target = new BinanceTradeInfo();
        target.copy(source);
        return target;
    }

    private List<BinanceTradeInfo> sanitizeTrades(List<BinanceTradeInfo> trades,
                                                  BinanceTradeStatsInfoVO statsInfo,
                                                  String tradeType) {
        if (CollectionUtils.isEmpty(trades)) {
            return new ArrayList<>();
        }
        List<BinanceTradeInfo> validTrades = trades.stream()
                .filter(this::isValidTrade)
                .collect(Collectors.toCollection(ArrayList::new));
        int invalidCount = trades.size() - validTrades.size();
        if (invalidCount > 0) {
            addWarning(statsInfo, "部分现货历史交易数据无效，已忽略");
            log.warn("忽略无效现货交易: type={}, count={}", tradeType, invalidCount);
        }
        return validTrades;
    }

    private boolean isValidTrade(BinanceTradeInfo trade) {
        return trade != null
                && trade.getQty() != null && trade.getQty().compareTo(BigDecimal.ZERO) > 0
                && trade.getPrice() != null && trade.getPrice().compareTo(BigDecimal.ZERO) > 0
                && trade.getTime() != null;
    }

    private void addWarning(BinanceTradeStatsInfoVO statsInfo, String warning) {
        if (!statsInfo.getWarnings().contains(warning)) {
            statsInfo.getWarnings().add(warning);
        }
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
    public BinanceSpotHedgeContext createHedgeContext(Integer uid, String symbol) {
        BinanceTradeInfoQueryCriteria criteria = new BinanceTradeInfoQueryCriteria();
        criteria.setUid(uid);
        criteria.setSymbol(symbol);
        criteria.setIsBuyer(1);
        criteria.setOrderColumn("price");
        criteria.setOrderDirection(OrderDirectionEnum.ASC.getValue());
        return new BinanceSpotHedgeContext(binanceTradeInfoMapper.findAll(criteria));
    }

    @Override
    public BinanceSpotHedgedTradeStatsInfoVO hedgedStats(BinanceSpotHedgeContext hedgeContext) {
        BinanceSpotHedgedTradeStatsInfoVO statsInfo = new BinanceSpotHedgedTradeStatsInfoVO();

        List<BinanceTradeInfo> hedgedTradeInfo = hedgeContext.getHedgedTrades();
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
