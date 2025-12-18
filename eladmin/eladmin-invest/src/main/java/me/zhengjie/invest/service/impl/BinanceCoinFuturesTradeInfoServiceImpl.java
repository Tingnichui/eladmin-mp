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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceCoinFuturesTradeInfo;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;
import me.zhengjie.invest.domain.vo.BinanceCoinFuturesTradeInfoQueryCriteria;
import me.zhengjie.invest.domain.vo.BinanceFuturesTradeStatsInfoVO;
import me.zhengjie.invest.mapper.BinanceCoinFuturesTradeInfoMapper;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceCoinFuturesTradeInfoService;
import me.zhengjie.invest.service.BinanceTradeInfoExtService;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceCoinFuturesUtil;
import me.zhengjie.invest.util.TradeMatcherUtil;
import me.zhengjie.utils.FileUtil;
import me.zhengjie.utils.PageResult;
import me.zhengjie.utils.PageUtil;
import me.zhengjie.utils.RedisUtils;
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
 * @date 2025-11-23
 **/
@Service
@RequiredArgsConstructor
public class BinanceCoinFuturesTradeInfoServiceImpl extends ServiceImpl<BinanceCoinFuturesTradeInfoMapper, BinanceCoinFuturesTradeInfo> implements BinanceCoinFuturesTradeInfoService {

    @Resource
    private BinanceCoinFuturesTradeInfoMapper binanceCoinFuturesTradeInfoMapper;
    @Resource
    private BinanceAccountInfoService binanceAccountInfoService;
    @Resource
    private BinanceCoinFuturesUtil binanceCoinFuturesUtil;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private BinanceTradeInfoService binanceTradeInfoService;
    @Resource
    private BinanceTradeInfoExtService binanceTradeInfoExtService;

    @Override
    public PageResult<BinanceCoinFuturesTradeInfo> queryAll(BinanceCoinFuturesTradeInfoQueryCriteria criteria, Page<Object> page) {
        return PageUtil.toPage(binanceCoinFuturesTradeInfoMapper.findAll(criteria, page));
    }

    @Override
    public List<BinanceCoinFuturesTradeInfo> queryAll(BinanceCoinFuturesTradeInfoQueryCriteria criteria) {
        return binanceCoinFuturesTradeInfoMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(BinanceCoinFuturesTradeInfo resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BinanceCoinFuturesTradeInfo resources) {
        BinanceCoinFuturesTradeInfo binanceCoinFuturesTradeInfo = getById(resources.getId());
        binanceCoinFuturesTradeInfo.copy(resources);
        saveOrUpdate(binanceCoinFuturesTradeInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<BinanceCoinFuturesTradeInfo> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (BinanceCoinFuturesTradeInfo binanceCoinFuturesTradeInfo : all) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("用户编号", binanceCoinFuturesTradeInfo.getUid());
            map.put("交易对", binanceCoinFuturesTradeInfo.getSymbol());
            map.put("订单 ID", binanceCoinFuturesTradeInfo.getOrderId());
            map.put("成交价格", binanceCoinFuturesTradeInfo.getPrice());
            map.put("成交数量", binanceCoinFuturesTradeInfo.getQty());
            map.put("成交额", binanceCoinFuturesTradeInfo.getBaseQty());
            map.put("手续费", binanceCoinFuturesTradeInfo.getCommission());
            map.put("手续费计价单位", binanceCoinFuturesTradeInfo.getCommissionAsset());
            map.put("成交时间", binanceCoinFuturesTradeInfo.getTime());
            map.put("是否为买方", binanceCoinFuturesTradeInfo.getBuyer());
            map.put("是否为挂单方", binanceCoinFuturesTradeInfo.getMaker());
            map.put("实现盈亏", binanceCoinFuturesTradeInfo.getRealizedPnl());
            map.put("买卖方向", binanceCoinFuturesTradeInfo.getSide());
            map.put("持仓方向", binanceCoinFuturesTradeInfo.getPositionSide());
            map.put("标的交易对", binanceCoinFuturesTradeInfo.getPair());
            map.put("保证金币种", binanceCoinFuturesTradeInfo.getMarginAsset());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    @Override
    public void sync() {
        // 查询所有账号
        List<BinanceAccountInfo> accountInfoList = binanceAccountInfoService.listUseApiAccount();
        for (BinanceAccountInfo accountInfo : accountInfoList) {
            BinanceAccountContextHolder.runWith(accountInfo, () -> {
                List<BinanceCoinFuturesTradeInfo> orderInfoList = binanceCoinFuturesUtil.userTrades(BinanceEnum.SYMBOL.BTCUSD_PERP, null, null);
                if (CollectionUtils.isEmpty(orderInfoList)) {
                    return;
                }

                // 查询已经在库中的订单
                Set<Long> existIdSet = this.list(
                        Wrappers.lambdaQuery(BinanceCoinFuturesTradeInfo.class)
                                .select(BinanceCoinFuturesTradeInfo::getId)
                                .in(BinanceCoinFuturesTradeInfo::getId, orderInfoList.stream().map(BinanceCoinFuturesTradeInfo::getId).collect(Collectors.toSet()))
                ).stream().map(BinanceCoinFuturesTradeInfo::getId).collect(Collectors.toSet());

                // 过滤掉已存在的订单
                List<BinanceCoinFuturesTradeInfo> newOrders = orderInfoList.stream()
                        .filter(order -> !existIdSet.contains(order.getId()))
                        .peek(order -> order.setUid(accountInfo.getUid()))
                        .collect(Collectors.toList());

                // 保存新订单
                this.saveOrUpdateBatch(newOrders);

            });
        }
    }

    @Override
    public Date getLastPosCloseTime() {
        final String key = "BINANCE:COIN_FUTURES:LAST_POS_CLOSE_TIME";
        Date lastPosCloseTime = (Date) redisUtils.get(key);
        List<BinanceCoinFuturesTradeInfo> binanceFuturesTradeInfoList = baseMapper.selectList(
                Wrappers.lambdaQuery(BinanceCoinFuturesTradeInfo.class)
                        .gt(null != lastPosCloseTime, BinanceCoinFuturesTradeInfo::getTime, lastPosCloseTime)
                        .orderByAsc(BinanceCoinFuturesTradeInfo::getTime)
        );

        BigDecimal pos = BigDecimal.ZERO;
        for (BinanceCoinFuturesTradeInfo tradeInfo : binanceFuturesTradeInfoList) {
            // 卖开仓 买减仓
            if (tradeInfo.getBuyer().equals(1)) {
                pos = pos.subtract(tradeInfo.getQty());
            } else {
                pos = pos.add(tradeInfo.getQty());
            }
            if (pos.compareTo(BigDecimal.ZERO) == 0) {
                redisUtils.set(key, tradeInfo.getTime());
            }
        }

        return (Date) redisUtils.get(key);
    }

    @Override
    public BinanceFuturesTradeStatsInfoVO stats() {
        BinanceFuturesTradeStatsInfoVO statsInfoVO = new BinanceFuturesTradeStatsInfoVO();

        // 当前仓位
        Date lastPosCloseTime = this.getLastPosCloseTime();
        final boolean side = false;


        // 开仓 做空空单
        List<BinanceCoinFuturesTradeInfo> openList = this.list(
                Wrappers.lambdaQuery(BinanceCoinFuturesTradeInfo.class)
                        .gt(BinanceCoinFuturesTradeInfo::getTime, lastPosCloseTime)
                        .eq(BinanceCoinFuturesTradeInfo::getSide, "SELL")
                        .eq(BinanceCoinFuturesTradeInfo::getPositionSide, "SHORT")
                        .orderByAsc(BinanceCoinFuturesTradeInfo::getTime)
        );

        // 平仓 做空多单
        List<BinanceCoinFuturesTradeInfo> closeList = this.list(
                Wrappers.lambdaQuery(BinanceCoinFuturesTradeInfo.class)
                        .gt(BinanceCoinFuturesTradeInfo::getTime, lastPosCloseTime)
                        .eq(BinanceCoinFuturesTradeInfo::getSide, "BUY")
                        .eq(BinanceCoinFuturesTradeInfo::getPositionSide, "SHORT")
                        .orderByAsc(BinanceCoinFuturesTradeInfo::getTime)
        );

        // 盈利交易匹配
        {

            List<MatchedTradeInfo> matchedList = TradeMatcherUtil.matchTrades(
                    side,
                    "0.0005",
                    openList,
                    closeList,
                    BinanceCoinFuturesTradeInfo::getBaseQty,
                    BinanceCoinFuturesTradeInfo::setBaseQty,
                    BinanceCoinFuturesTradeInfo::getPrice,
                    BinanceCoinFuturesTradeInfo::getTime,
                    match -> {
                        return match.getNetPnl().compareTo(BigDecimal.ZERO) >= 0;
                    }
            );

            // 盈亏
            statsInfoVO.setPnl(matchedList.stream().map(MatchedTradeInfo::getPnl).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 手续费
            statsInfoVO.setFee(matchedList.stream().map(MatchedTradeInfo::getFee).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 净盈亏
            statsInfoVO.setNetPnl(matchedList.stream().map(MatchedTradeInfo::getNetPnl).reduce(BigDecimal.ZERO, BigDecimal::add));

            if (CollectionUtils.isNotEmpty(openList)) {
                // 持仓金额
                statsInfoVO.setPosAmount(openList.stream().map(v -> v.getBaseQty().multiply(v.getPrice())).reduce(BigDecimal.ZERO, BigDecimal::add));
                // 持仓数量
                statsInfoVO.setPosQty(openList.stream().map(BinanceCoinFuturesTradeInfo::getBaseQty).reduce(BigDecimal.ZERO, BigDecimal::add));
                // 持仓均价
                statsInfoVO.setPosAvgPrice(statsInfoVO.getPosAmount().divide(statsInfoVO.getPosQty(), 8, RoundingMode.HALF_UP));
            }

        }


        // 现货止损交易匹配
        {
            // 获取当前合约价格
            BigDecimal currentPrice = binanceCoinFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSD_PERP);
            List<MatchedTradeInfo> matchedList = TradeMatcherUtil.matchTrades(side, "0.001", openList, BinanceCoinFuturesTradeInfo::getBaseQty, BinanceCoinFuturesTradeInfo::getPrice, currentPrice,
                    matched -> {
                        // 亏损单找现货做对冲止损
                        if (matched.getNetPnl().compareTo(BigDecimal.ZERO) <= 0) {
                            BigDecimal totalHedgedAmount = BigDecimal.ZERO;

                            BigDecimal qty = matched.getQty();
                            // 查询现货止损单
                            List<BinanceTradeInfo> spotInfos = binanceTradeInfoService.list4hedge(matched.getOpenPrice(), matched.getOpenPrice().add(new BigDecimal("1000")), qty, 100);
                            for (BinanceTradeInfo spot : spotInfos) {
                                // 对冲数量
                                BigDecimal matchQty = spot.getNetQty().min(qty);
                                if (matchQty.compareTo(BigDecimal.ZERO) <= 0) {
                                    continue;
                                }

                                // 记录对冲价格 和 对冲数量
                                totalHedgedAmount = totalHedgedAmount.add(spot.getPrice().multiply(matchQty));
                                // 标记对冲
                                binanceTradeInfoExtService.changeHedgedFlag(spot.getId(), matchQty);
                                // 扣减数量
                                qty = qty.subtract(matchQty);
                                if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                                    break;
                                }
                            }

                            BigDecimal hedgedQty = matched.getQty().subtract(qty);
                            if (hedgedQty.compareTo(BigDecimal.ZERO) > 0) {
                                matched.setClosePrice(totalHedgedAmount.divide(hedgedQty, 8, RoundingMode.HALF_UP));
                            } else {
                                matched.setClosePrice(BigDecimal.ZERO);
                            }

                        }
                    });

            // 计算止损金额
            statsInfoVO.setStopLossAmount(matchedList.stream().map(MatchedTradeInfo::getPnl).filter(v -> v.compareTo(BigDecimal.ZERO) <= 0).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 未平仓的交易
            statsInfoVO.setTradeList(matchedList.stream().sorted(Comparator.comparing(MatchedTradeInfo::getOpenPrice).reversed()).collect(Collectors.toList()));
            // 资金费
            statsInfoVO.setFundingFee(this.calculatePositionFundingFee(BinanceEnum.SYMBOL.BTCUSD_PERP).multiply(currentPrice));

        }

        return statsInfoVO;
    }

    @Override
    public BigDecimal calculatePositionFundingFee(BinanceEnum.SYMBOL symbol) {
        Date startTime = getLastPosCloseTime();
        String incomeType = "FUNDING_FEE";
        List<JSONObject> list = binanceCoinFuturesUtil.listIncome(symbol, startTime.getTime(), incomeType);
        return list.stream().map(v -> v.getBigDecimal("income")).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}