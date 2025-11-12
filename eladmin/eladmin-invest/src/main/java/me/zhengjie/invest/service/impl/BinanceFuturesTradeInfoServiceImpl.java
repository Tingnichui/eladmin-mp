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
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceFuturesTradeInfo;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.BinanceTradeInfoExt;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;
import me.zhengjie.invest.domain.vo.BinanceFuturesTradeStatsInfoVO;
import me.zhengjie.invest.domain.vo.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceTradeInfoExtService;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.invest.util.BinanceUsdFuturesUtil;
import me.zhengjie.invest.util.TradeMatcherUtil;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.BinanceFuturesTradeInfoService;
import me.zhengjie.invest.domain.vo.BinanceFuturesTradeInfoQueryCriteria;
import me.zhengjie.invest.mapper.BinanceFuturesTradeInfoMapper;
import me.zhengjie.utils.RedisUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.zhengjie.utils.PageUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;

import me.zhengjie.utils.PageResult;

/**
* @description 服务实现
* @author genghui
* @date 2025-10-17
**/
@Service
@RequiredArgsConstructor
public class BinanceFuturesTradeInfoServiceImpl extends ServiceImpl<BinanceFuturesTradeInfoMapper, BinanceFuturesTradeInfo> implements BinanceFuturesTradeInfoService {

    private final BinanceFuturesTradeInfoMapper binanceFuturesTradeInfoMapper;
    private final BinanceUsdFuturesUtil binanceUsdFuturesUtil;
    private final BinanceSpotUtil binanceSpotUtil;
    private final BinanceAccountInfoService binanceAccountInfoService;
    private final RedisUtils redisUtils;
    private final BinanceTradeInfoExtService binanceTradeInfoExtService;
    private final BinanceTradeInfoService binanceTradeInfoService;

    @Override
    public PageResult<BinanceFuturesTradeInfo> queryAll(BinanceFuturesTradeInfoQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(binanceFuturesTradeInfoMapper.findAll(criteria, page));
    }

    @Override
    public List<BinanceFuturesTradeInfo> queryAll(BinanceFuturesTradeInfoQueryCriteria criteria){
        return binanceFuturesTradeInfoMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(BinanceFuturesTradeInfo resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BinanceFuturesTradeInfo resources) {
        BinanceFuturesTradeInfo binanceFuturesTradeInfo = getById(resources.getId());
        binanceFuturesTradeInfo.copy(resources);
        saveOrUpdate(binanceFuturesTradeInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<BinanceFuturesTradeInfo> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (BinanceFuturesTradeInfo binanceFuturesTradeInfo : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("用户编号", binanceFuturesTradeInfo.getUid());
            map.put("交易对", binanceFuturesTradeInfo.getSymbol());
            map.put("订单 ID", binanceFuturesTradeInfo.getOrderId());
            map.put("成交价格", binanceFuturesTradeInfo.getPrice());
            map.put("成交数量", binanceFuturesTradeInfo.getQty());
            map.put("成交额", binanceFuturesTradeInfo.getQuoteQty());
            map.put("手续费", binanceFuturesTradeInfo.getCommission());
            map.put("手续费计价单位", binanceFuturesTradeInfo.getCommissionAsset());
            map.put("成交时间", binanceFuturesTradeInfo.getTime());
            map.put("是否为买方", binanceFuturesTradeInfo.getBuyer());
            map.put("是否为挂单方", binanceFuturesTradeInfo.getMaker());
            map.put("实现盈亏", binanceFuturesTradeInfo.getRealizedPnl());
            map.put("买卖方向", binanceFuturesTradeInfo.getSide());
            map.put("持仓方向", binanceFuturesTradeInfo.getPositionSide());
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
                List<BinanceFuturesTradeInfo> orderInfoList = binanceUsdFuturesUtil.userTrades(BinanceEnum.SYMBOL.BTCUSDT, null, null);
                if (CollectionUtils.isEmpty(orderInfoList)) {
                    return;
                }

                // 查询已经在库中的订单
                Set<Long> existOrderIdSet = this.list(
                        Wrappers.lambdaQuery(BinanceFuturesTradeInfo.class)
                                .select(BinanceFuturesTradeInfo::getOrderId)
                                .in(BinanceFuturesTradeInfo::getOrderId, orderInfoList.stream().map(BinanceFuturesTradeInfo::getOrderId).collect(Collectors.toSet()))
                ).stream().map(BinanceFuturesTradeInfo::getOrderId).collect(Collectors.toSet());

                // 过滤掉已存在的订单
                List<BinanceFuturesTradeInfo> newOrders = orderInfoList.stream()
                        .filter(order -> !existOrderIdSet.contains(order.getOrderId()))
                        .peek(order -> order.setUid(accountInfo.getUid()))
                        .collect(Collectors.toList());

                // 保存新订单
                this.saveOrUpdateBatch(newOrders);

            });
        }
    }

    @Override
    public Date getLastPosCloseTime() {
        final String key = "BINANCE:FUTURES:LAST_POS_CLOSE_TIME";
        Date lastPosCloseTime = (Date) redisUtils.get(key);
        List<BinanceFuturesTradeInfo> binanceFuturesTradeInfoList = binanceFuturesTradeInfoMapper.selectList(
                Wrappers.lambdaQuery(BinanceFuturesTradeInfo.class)
                        .gt(null != lastPosCloseTime, BinanceFuturesTradeInfo::getTime, lastPosCloseTime)
                        .orderByAsc(BinanceFuturesTradeInfo::getTime)
        );

        BigDecimal pos = BigDecimal.ZERO;
        for (BinanceFuturesTradeInfo tradeInfo : binanceFuturesTradeInfoList) {
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
    public BinanceFuturesTradeStatsInfoVO syncFuturesHedge() {
        BinanceFuturesTradeStatsInfoVO statsInfoVO = new BinanceFuturesTradeStatsInfoVO();

        // 当前仓位
        Date lastPosCloseTime = this.getLastPosCloseTime();
        final boolean side = false;


        // 开仓 做空空单
        List<BinanceFuturesTradeInfo> openList = binanceFuturesTradeInfoMapper.selectList(
                Wrappers.lambdaQuery(BinanceFuturesTradeInfo.class)
                        .gt(BinanceFuturesTradeInfo::getTime, lastPosCloseTime)
                        .eq(BinanceFuturesTradeInfo::getSide, "SELL")
                        .eq(BinanceFuturesTradeInfo::getPositionSide, "SHORT")
                        .orderByAsc(BinanceFuturesTradeInfo::getTime)
        );

        // 平仓 做空多单
        List<BinanceFuturesTradeInfo> closeList = binanceFuturesTradeInfoMapper.selectList(
                Wrappers.lambdaQuery(BinanceFuturesTradeInfo.class)
                        .gt(BinanceFuturesTradeInfo::getTime, lastPosCloseTime)
                        .eq(BinanceFuturesTradeInfo::getSide, "BUY")
                        .eq(BinanceFuturesTradeInfo::getPositionSide, "SHORT")
                        .orderByAsc(BinanceFuturesTradeInfo::getTime)
        );

        // 盈利交易匹配
        {

            List<MatchedTradeInfo> matchedList = TradeMatcherUtil.matchTrades(
                    side,
                    "0.0005",
                    openList,
                    closeList,
                    BinanceFuturesTradeInfo::getQty,
                    BinanceFuturesTradeInfo::setQty,
                    BinanceFuturesTradeInfo::getPrice,
                    BinanceFuturesTradeInfo::getTime,
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
                statsInfoVO.setPosAmount(openList.stream().map(v -> v.getQty().multiply(v.getPrice())).reduce(BigDecimal.ZERO, BigDecimal::add));
                // 持仓数量
                statsInfoVO.setPosQty(openList.stream().map(BinanceFuturesTradeInfo::getQty).reduce(BigDecimal.ZERO, BigDecimal::add));
                // 持仓均价
                statsInfoVO.setPosAvgPrice(statsInfoVO.getPosAmount().divide(statsInfoVO.getPosQty(), 8, RoundingMode.HALF_UP));
            }

        }


        // 现货止损交易匹配
        {
            // 获取当前合约价格
            BigDecimal currentPrice = binanceUsdFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSDT);

            // 所有都标记未锁仓
            binanceTradeInfoExtService.getBaseMapper().update(null,
                    Wrappers.lambdaUpdate(BinanceTradeInfoExt.class)
                            .set(BinanceTradeInfoExt::getHedgedFlag, 0)
            );

            List<MatchedTradeInfo> matchedList = TradeMatcherUtil.matchTrades(side, "0.001", openList, BinanceFuturesTradeInfo::getQty, BinanceFuturesTradeInfo::getPrice, currentPrice,
                    matched -> {
                        // 亏损单找现货做对冲止损
                        if (matched.getNetPnl().compareTo(BigDecimal.ZERO) <= 0) {
                            // 查询现货止损单
                            List<BinanceTradeInfo> binanceTradeInfos = binanceTradeInfoService.list4hedge(matched.getOpenPrice(), matched.getOpenPrice().add(new BigDecimal("1000")), matched.getQty());
                            if (CollectionUtils.isNotEmpty(binanceTradeInfos)) {
                                BinanceTradeInfo buy = binanceTradeInfos.get(0);
                                matched.setClosePrice(buy.getPrice());
                                binanceTradeInfoExtService.changeHedgedFlag(buy.getOrderId());
                            } else {
                                matched.setClosePrice(BigDecimal.ZERO);
                            }
                        }
                    });

            // 计算止损金额
            statsInfoVO.setStopLossAmount(matchedList.stream().map(MatchedTradeInfo::getPnl).filter(v -> v.compareTo(BigDecimal.ZERO) <= 0).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 未平仓的交易
            statsInfoVO.setOpenTradeList(matchedList.stream().sorted(Comparator.comparing(MatchedTradeInfo::getOpenPrice).reversed()).collect(Collectors.toList()));

        }


        // 锁仓统计
        {
            BinanceTradeInfoQueryCriteria criteria = new BinanceTradeInfoQueryCriteria();
            criteria.setHedgedFlag(1);
            List<BinanceTradeInfo> hedgedTradeInfo = binanceTradeInfoService.queryAll(criteria);
            if (CollectionUtils.isNotEmpty(hedgedTradeInfo)) {
                // 锁仓总额
                statsInfoVO.setHedgedAmount(hedgedTradeInfo.stream().map(BinanceTradeInfo::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                // 锁仓数量
                statsInfoVO.setHedgedQty(hedgedTradeInfo.stream().map(BinanceTradeInfo::getQty).reduce(BigDecimal.ZERO, BigDecimal::add));
                // 锁仓均价
                statsInfoVO.setHedgedAvgPrice(NumberUtil.div(statsInfoVO.getHedgedAmount(), statsInfoVO.getHedgedQty()));
            }
        }


        return statsInfoVO;
    }


}
