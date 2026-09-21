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
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceSpotTradeMatchState;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceOrderApiDto;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceOrderVO;
import me.zhengjie.invest.domain.dto.BinanceSpotHedgedTradeStatsInfoVO;
import me.zhengjie.invest.domain.dto.BinanceSpotTradeStatsAggregate;
import me.zhengjie.invest.domain.dto.BinanceSpotOrderRequest;
import me.zhengjie.invest.domain.dto.BinanceSpotOpenOrderDto;
import me.zhengjie.invest.domain.dto.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.domain.dto.BinanceTradeStatsInfoVO;
import me.zhengjie.invest.mapper.BinanceTradeInfoMapper;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchMapper;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchStateMapper;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceSpotTradeMatcherService;
import me.zhengjie.invest.service.BinanceTradeInfoExtService;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.invest.service.support.BinanceSpotHedgeContext;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.invest.util.BinanceUsdFuturesUtil;
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
import java.util.concurrent.atomic.AtomicReference;
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

    private static final int SYNC_PAGE_SIZE = 1000;

    @Resource
    private BinanceTradeInfoMapper binanceTradeInfoMapper;
    @Resource
    private BinanceSpotTradeMatchMapper binanceSpotTradeMatchMapper;
    @Resource
    private BinanceSpotTradeMatchStateMapper binanceSpotTradeMatchStateMapper;
    @Resource
    private BinanceAccountInfoService binanceAccountInfoService;
    @Resource
    private BinanceSpotUtil binanceSpotUtil;
    @Resource
    private BinanceSpotTradeMatcherService binanceSpotTradeMatcherService;
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
            syncTradeInfo(accountInfo, symbol);
        }
    }

    @Override
    public int syncTradeInfo(BinanceAccountInfo accountInfo, String symbol) {
        int[] syncedCount = {0};
        BinanceAccountContextHolder.runWith(accountInfo, () -> {
            List<BinanceTradeInfo> latestTrades = this.list(
                    Wrappers.<BinanceTradeInfo>query()
                            .select("id")
                            .eq("uid", accountInfo.getUid())
                            .eq("symbol", symbol)
                            .orderByDesc("id")
                            .last("limit 1")
            );
            long fromId = latestTrades.isEmpty() ? 0L : latestTrades.get(0).getId() + 1L;
            while (true) {
                List<BinanceTradeInfo> orderInfoList = binanceSpotUtil.getMyTrades(
                        symbol, fromId, SYNC_PAGE_SIZE);
                if (CollectionUtils.isEmpty(orderInfoList)) {
                    break;
                }
                Set<Long> pageIds = orderInfoList.stream().map(BinanceTradeInfo::getId)
                        .filter(Objects::nonNull).collect(Collectors.toSet());
                if (pageIds.isEmpty()) {
                    throw new IllegalStateException("币安现货成交数据缺少 ID");
                }
                Set<Long> existIdSet = this.list(
                        Wrappers.<BinanceTradeInfo>query()
                                .select("id")
                                .eq("uid", accountInfo.getUid())
                                .eq("symbol", symbol)
                                .in("id", pageIds)
                ).stream().map(BinanceTradeInfo::getId).collect(Collectors.toSet());
                List<BinanceTradeInfo> newOrders = orderInfoList.stream()
                        .filter(order -> order.getId() != null && !existIdSet.contains(order.getId()))
                        .peek(order -> order.setUid(accountInfo.getUid()))
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(newOrders)) {
                    this.saveOrUpdateBatch(newOrders);
                    syncedCount[0] += newOrders.size();
                }
                long nextFromId = Collections.max(pageIds) + 1L;
                if (nextFromId <= fromId) {
                    throw new IllegalStateException("币安现货成交分页未向前推进");
                }
                fromId = nextFromId;
                if (orderInfoList.size() < SYNC_PAGE_SIZE) {
                    break;
                }
            }
        });
        binanceSpotTradeMatcherService.initializeAndMatch(accountInfo.getUid(), symbol);
        return syncedCount[0];
    }

    @Override
    public BinanceTradeStatsInfoVO stats(BinanceTradeInfoQueryCriteria criteria) {
        return stats(criteria, null, true);
    }

    @Override
    public BinanceTradeStatsInfoVO stats(BinanceTradeInfoQueryCriteria criteria, BigDecimal currentPrice) {
        return stats(criteria, currentPrice, false);
    }

    private BinanceTradeStatsInfoVO stats(BinanceTradeInfoQueryCriteria criteria,
                                          BigDecimal realtimePrice,
                                          boolean loadRealtimePrice) {
        final String key = "SPOT_LAST_NET_PNL:" + criteria.getUid() + ":" + criteria.getSymbol();
        BinanceTradeStatsInfoVO statsInfoVO = new BinanceTradeStatsInfoVO();
        statsInfoVO.setLastNetPnl((BigDecimal) redisUtils.get(key));
        final boolean side = true;
        final String feeRate = "0.001";
        BinanceSpotTradeStatsAggregate aggregate = binanceSpotTradeMatchMapper.aggregateStats(
                criteria.getUid(), criteria.getSymbol());
        if (aggregate == null) {
            aggregate = new BinanceSpotTradeStatsAggregate();
        }
        statsInfoVO.setTotalBuyAmount(zeroIfNull(aggregate.getTotalBuyAmount()));
        statsInfoVO.setTotalSellAmount(zeroIfNull(aggregate.getTotalSellAmount()));
        statsInfoVO.setPnl(zeroIfNull(aggregate.getPnl()));
        statsInfoVO.setFee(zeroIfNull(aggregate.getFee()));
        statsInfoVO.setNetPnl(zeroIfNull(aggregate.getNetPnl()));
        statsInfoVO.setRoi(statsInfoVO.getTotalBuyAmount().compareTo(BigDecimal.ZERO) > 0
                ? statsInfoVO.getNetPnl().divide(statsInfoVO.getTotalBuyAmount(), 8, RoundingMode.HALF_UP)
                : null);
        redisUtils.set(key, statsInfoVO.getNetPnl());

        BigDecimal unmatchedSellQty = zeroIfNull(binanceSpotTradeMatchStateMapper.sumStatsUnmatchedSellQty(
                criteria.getUid(), criteria.getSymbol()));
        statsInfoVO.setUnmatchedSellQty(unmatchedSellQty);
        if (unmatchedSellQty.compareTo(BigDecimal.ZERO) > 0) {
            addWarning(statsInfoVO, "部分卖出成交缺少可匹配的历史买入");
        }

        List<BinanceSpotTradeMatchState> openList = binanceSpotTradeMatchStateMapper.findStatsOpenBuys(
                criteria.getUid(), criteria.getSymbol());
        if (openList == null) {
            openList = Collections.emptyList();
        }
        BigDecimal totalWaitSellAmount = openList.stream()
                .map(b -> zeroIfNull(b.getRemainingQty()).multiply(zeroIfNull(b.getPrice())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statsInfoVO.setPosAmount(totalWaitSellAmount);
        BigDecimal totalWaitSellQty = openList.stream()
                .map(BinanceSpotTradeMatchState::getRemainingQty)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statsInfoVO.setPosQty(totalWaitSellQty);
        statsInfoVO.setPosAvgPrice(totalWaitSellQty.compareTo(BigDecimal.ZERO) > 0
                ? totalWaitSellAmount.divide(totalWaitSellQty, 8, RoundingMode.HALF_UP)
                : null);

        if (openList.isEmpty()) {
            return statsInfoVO;
        }

        try {
            BigDecimal currentPrice = loadRealtimePrice
                    ? binanceSpotUtil.getPrice(BinanceEnum.SYMBOL.valueOf(criteria.getSymbol()))
                    : realtimePrice;
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("现货价格为空或无效");
            }
            statsInfoVO.setCurrentSpotPrice(currentPrice);


            List<MatchedTradeInfo> matchedTradeInfos = openList.stream().map(position -> {
                MatchedTradeInfo trade = new MatchedTradeInfo(side, feeRate);
                BigDecimal coreQty = zeroIfNull(position.getActiveCoreQty());
                trade.setTradeId(position.getTradeId());
                trade.setOrderId(position.getOrderId());
                trade.setCorePositionId(position.getCorePositionId());
                trade.setCoreQty(coreQty);
                trade.setAvailableQty(zeroIfNull(position.getRemainingQty()).subtract(coreQty));
                trade.setCoreLockedAt(position.getCoreLockedAt());
                trade.setQty(position.getRemainingQty());
                trade.setOpenPrice(position.getPrice());
                trade.setOpenTime(position.getTradeTime());
                trade.setClosePrice(currentPrice);
                return trade;
            }).collect(Collectors.toList());
            // 持仓盈利
            statsInfoVO.setHoldingProfit(matchedTradeInfos.stream().map(MatchedTradeInfo::getNetPnl).filter(netPnl -> netPnl.compareTo(BigDecimal.ZERO) > 0).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 持仓亏损
            statsInfoVO.setHoldingLoss(matchedTradeInfos.stream().map(MatchedTradeInfo::getNetPnl).filter(netPnl -> netPnl.compareTo(BigDecimal.ZERO) <= 0).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 持仓盈亏
            statsInfoVO.setHoldingProfitLoss(matchedTradeInfos.stream().map(MatchedTradeInfo::getNetPnl).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 持仓订单
            // 按未平仓买入批次逐笔返回，保留真实成交时间；价格区间聚合由前端按需完成。
            statsInfoVO.setTradeList(matchedTradeInfos.stream()
                    .sorted(Comparator.comparing(MatchedTradeInfo::getOpenTime))
                    .collect(Collectors.toList()));

        } catch (Exception e) {
            statsInfoVO.setCurrentSpotPrice(null);
            statsInfoVO.setTradeList(Collections.emptyList());
            addWarning(statsInfoVO, "现货持仓实时估值暂不可用");
            log.warn("现货持仓实时估值失败: uid={}, symbol={}, error={}",
                    criteria.getUid(), criteria.getSymbol(), e.getClass().getSimpleName());
        }

        return statsInfoVO;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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

    @Override
    public Long createSpotOrder(BinanceSpotOrderRequest request) {
        BinanceEnum.SYMBOL symbol = requireSpotSymbol(request.getSymbol());
        if (request.getType() != BinanceEnum.TYPE.STOP_LOSS_LIMIT
                && request.getType() != BinanceEnum.TYPE.TAKE_PROFIT_LIMIT) {
            throw new BadRequestException("仅支持限价止盈或限价止损订单");
        }
        if (request.getPriceMode() == BinanceEnum.PRICE_MODE.FIXED && request.getPrice() == null) {
            throw new BadRequestException("固定委托价模式必须填写委托价");
        }
        if (request.getPriceMode() == BinanceEnum.PRICE_MODE.OPPONENT_FIRST && request.getPrice() != null) {
            throw new BadRequestException("对手价1模式不能填写固定委托价");
        }

        BinanceAccountInfo accountInfo = requireAvailableAccount(request.getUid());

        BinanceOrderApiDto apiDto = new BinanceOrderApiDto();
        apiDto.setSymbol(symbol.name());
        apiDto.setSide(request.getSide());
        apiDto.setType(request.getType());
        apiDto.setTimeInForce(BinanceEnum.TIME_IN_FORCE.GTC);
        apiDto.setQuantity(request.getQuantity());
        apiDto.setStopPrice(request.getStopPrice());
        if (request.getPriceMode() == BinanceEnum.PRICE_MODE.FIXED) {
            apiDto.setPrice(request.getPrice());
        } else {
            apiDto.setPegPriceType(BinanceEnum.PEG_PRICE_TYPE.MARKET_PEG);
        }

        Long[] orderId = new Long[1];
        BinanceAccountContextHolder.runWith(accountInfo, () -> orderId[0] = binanceSpotUtil.order(apiDto));
        return orderId[0];
    }

    @Override
    public List<BinanceSpotOpenOrderDto> listSpotOpenOrders(Integer uid, String symbolValue) {
        BinanceEnum.SYMBOL symbol = requireSpotSymbol(symbolValue);
        BinanceAccountInfo accountInfo = requireAvailableAccount(uid);
        AtomicReference<List<BinanceSpotOpenOrderDto>> result = new AtomicReference<>();
        BinanceAccountContextHolder.runWith(accountInfo,
                () -> result.set(binanceSpotUtil.listOpenOrders(symbol.name())));
        return result.get();
    }

    private BinanceEnum.SYMBOL requireSpotSymbol(String symbolValue) {
        BinanceEnum.SYMBOL symbol;
        try {
            symbol = BinanceEnum.SYMBOL.valueOf(symbolValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("不支持的交易对");
        }
        if (!Integer.valueOf(0).equals(symbol.getType())) {
            throw new BadRequestException("请选择现货交易对");
        }
        return symbol;
    }

    private BinanceAccountInfo requireAvailableAccount(Integer uid) {
        BinanceAccountInfo accountInfo = binanceAccountInfoService.getAccountByUid(uid);
        if (accountInfo == null || !Integer.valueOf(1).equals(accountInfo.getApiValidFlag())) {
            throw new BadRequestException("账户API不可用");
        }
        return accountInfo;
    }

}
