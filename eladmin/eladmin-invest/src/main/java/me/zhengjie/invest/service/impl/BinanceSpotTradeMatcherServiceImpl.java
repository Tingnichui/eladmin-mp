package me.zhengjie.invest.service.impl;

import lombok.RequiredArgsConstructor;
import me.zhengjie.invest.domain.BinanceSpotTradeMatch;
import me.zhengjie.invest.domain.BinanceSpotTradeMatchState;
import me.zhengjie.invest.domain.dto.BinanceSpotTradeMatchResult;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchMapper;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchStateMapper;
import me.zhengjie.invest.service.BinanceSpotTradeMatcherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 将现货 FIFO 撮合结果固化到数据库，现货统计直接读取固化结果与剩余持仓。
 */
@Service
@RequiredArgsConstructor
public class BinanceSpotTradeMatcherServiceImpl implements BinanceSpotTradeMatcherService {

    static final String STATUS_PENDING = "PENDING";
    static final String STATUS_PARTIAL = "PARTIAL";
    static final String STATUS_COMPLETED = "COMPLETED";
    static final String STATUS_EXCEPTION = "EXCEPTION";
    static final BigDecimal FEE_RATE = new BigDecimal("0.001");

    private final BinanceSpotTradeMatchStateMapper stateMapper;
    private final BinanceSpotTradeMatchMapper matchMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int initializeStates(Integer uid, String symbol) {
        validateScope(uid, symbol);
        return stateMapper.initializeFromTrades(uid, symbol);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BinanceSpotTradeMatchResult match(Integer uid, String symbol) {
        validateScope(uid, symbol);
        return matchInternal(uid, symbol);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BinanceSpotTradeMatchResult initializeAndMatch(Integer uid, String symbol) {
        validateScope(uid, symbol);
        int initializedCount = stateMapper.initializeFromTrades(uid, symbol);
        BinanceSpotTradeMatchResult result = matchInternal(uid, symbol);
        result.setInitializedCount(initializedCount);
        return result;
    }

    private BinanceSpotTradeMatchResult matchInternal(Integer uid, String symbol) {
        BinanceSpotTradeMatchResult result = new BinanceSpotTradeMatchResult();
        List<BinanceSpotTradeMatchState> sells = stateMapper.findPendingSellsForUpdate(uid, symbol);
        for (BinanceSpotTradeMatchState sell : sells) {
            matchSell(uid, symbol, sell, result);
        }
        return result;
    }

    private void matchSell(Integer uid,
                           String symbol,
                           BinanceSpotTradeMatchState sell,
                           BinanceSpotTradeMatchResult result) {
        List<BinanceSpotTradeMatchState> buys = stateMapper.findAvailableBuysForUpdate(
                uid, symbol, sell.getTradeTime(), sell.getTradeId());
        BigDecimal sellRemaining = positive(sell.getRemainingQty());

        for (BinanceSpotTradeMatchState buy : buys) {
            if (sellRemaining.signum() <= 0) {
                break;
            }
            BigDecimal buyRemaining = positive(buy.getRemainingQty());
            BigDecimal buyAvailable = buyRemaining.subtract(positive(buy.getActiveCoreQty()));
            if (buyAvailable.signum() <= 0) {
                continue;
            }

            BigDecimal matchedQty = buyAvailable.min(sellRemaining);
            matchMapper.insert(createMatch(uid, symbol, buy, sell, matchedQty));
            result.addMatch(matchedQty);

            BigDecimal newBuyRemaining = buyRemaining.subtract(matchedQty);
            BigDecimal newBuyMatched = positive(buy.getMatchedQty()).add(matchedQty);
            stateMapper.updateMatchProgress(
                    buy.getId(), newBuyMatched, newBuyRemaining, status(newBuyMatched, newBuyRemaining));
            buy.setMatchedQty(newBuyMatched);
            buy.setRemainingQty(newBuyRemaining);

            sellRemaining = sellRemaining.subtract(matchedQty);
            sell.setMatchedQty(positive(sell.getMatchedQty()).add(matchedQty));
            sell.setRemainingQty(sellRemaining);
        }

        String sellStatus;
        if (sellRemaining.signum() == 0) {
            sellStatus = STATUS_COMPLETED;
            result.setCompletedSellCount(result.getCompletedSellCount() + 1);
        } else {
            sellStatus = STATUS_EXCEPTION;
            result.setExceptionSellCount(result.getExceptionSellCount() + 1);
        }
        stateMapper.updateMatchProgress(
                sell.getId(), positive(sell.getMatchedQty()), sellRemaining, sellStatus);
    }

    private BinanceSpotTradeMatch createMatch(Integer uid,
                                               String symbol,
                                               BinanceSpotTradeMatchState buy,
                                               BinanceSpotTradeMatchState sell,
                                               BigDecimal matchedQty) {
        if (buy.getPrice() == null || sell.getPrice() == null) {
            throw new IllegalStateException("现货撮合成交价格不能为空");
        }
        BigDecimal buyAmount = matchedQty.multiply(buy.getPrice());
        BigDecimal sellAmount = matchedQty.multiply(sell.getPrice());
        BigDecimal pnl = sellAmount.subtract(buyAmount);
        BigDecimal fee = buyAmount.add(sellAmount).multiply(FEE_RATE);

        BinanceSpotTradeMatch match = new BinanceSpotTradeMatch();
        match.setUid(uid);
        match.setSymbol(symbol);
        match.setBuyTradeId(buy.getTradeId());
        match.setSellTradeId(sell.getTradeId());
        match.setMatchedQty(matchedQty);
        match.setBuyPrice(buy.getPrice());
        match.setSellPrice(sell.getPrice());
        match.setBuyTime(buy.getTradeTime());
        match.setSellTime(sell.getTradeTime());
        match.setBuyAmount(buyAmount);
        match.setSellAmount(sellAmount);
        match.setFeeRate(FEE_RATE);
        match.setPnl(pnl);
        match.setFee(fee);
        match.setNetPnl(pnl.subtract(fee));
        return match;
    }

    private String status(BigDecimal matchedQty, BigDecimal remainingQty) {
        if (remainingQty.signum() == 0) {
            return STATUS_COMPLETED;
        }
        return matchedQty.signum() > 0 ? STATUS_PARTIAL : STATUS_PENDING;
    }

    private BigDecimal positive(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    private void validateScope(Integer uid, String symbol) {
        if (uid == null) {
            throw new IllegalArgumentException("uid 不能为空");
        }
        if (!StringUtils.hasText(symbol)) {
            throw new IllegalArgumentException("symbol 不能为空");
        }
    }

}
