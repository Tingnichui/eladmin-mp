package me.zhengjie.invest.service.impl;

import lombok.RequiredArgsConstructor;
import me.zhengjie.invest.domain.BinanceSpotTradeMatch;
import me.zhengjie.invest.domain.BinanceSpotTradeMatchState;
import me.zhengjie.invest.domain.dto.BinanceSpotSellSourceDto;
import me.zhengjie.invest.domain.dto.BinanceSpotTradeMatchResult;
import me.zhengjie.invest.domain.dto.SpotActualFeeRebuildResult;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchMapper;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchStateMapper;
import me.zhengjie.invest.service.BinanceSpotTradeMatcherService;
import me.zhengjie.invest.service.support.BinanceSpotSellSourceStore;
import me.zhengjie.invest.service.support.SpotCommissionValuationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import javax.annotation.Resource;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    static final String STATUS_SOURCE_EXCEPTION = "SOURCE_EXCEPTION";
    private static final int MONEY_SCALE = 16;

    private final BinanceSpotTradeMatchStateMapper stateMapper;
    private final BinanceSpotTradeMatchMapper matchMapper;
    private final BinanceSpotSellSourceStore sellSourceStore;
    @Resource
    private SpotCommissionValuationService commissionValuationService;

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
        BinanceSpotSellSourceDto source = sell.getOrderId() == null
                ? null : sellSourceStore.find(uid, symbol, sell.getOrderId());
        if (source != null && !validSource(uid, symbol, sell.getOrderId(), source)) {
            markSellException(sell, result, STATUS_SOURCE_EXCEPTION);
            return;
        }
        List<BinanceSpotTradeMatchState> buys = findCandidateBuys(uid, symbol, sell, source);
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
            sellStatus = source == null ? STATUS_EXCEPTION : STATUS_SOURCE_EXCEPTION;
            result.setExceptionSellCount(result.getExceptionSellCount() + 1);
        }
        stateMapper.updateMatchProgress(
                sell.getId(), positive(sell.getMatchedQty()), sellRemaining, sellStatus);
    }

    private List<BinanceSpotTradeMatchState> findCandidateBuys(Integer uid,
                                                                String symbol,
                                                                BinanceSpotTradeMatchState sell,
                                                                BinanceSpotSellSourceDto source) {
        if (source == null) {
            return stateMapper.findAvailableBuysForUpdate(
                    uid, symbol, sell.getTradeTime(), sell.getTradeId());
        }
        if (source.getSourceType() == BinanceSpotSellSourceDto.SourceType.TRADE) {
            return stateMapper.findAvailableBuysByTradeIdForUpdate(
                    uid, symbol, source.getSourceTradeId(), sell.getTradeTime(), sell.getTradeId());
        }
        return stateMapper.findAvailableBuysByOrderIdForUpdate(
                uid, symbol, source.getSourceOrderId(), sell.getTradeTime(), sell.getTradeId());
    }

    private boolean validSource(Integer uid,
                                String symbol,
                                Long sellOrderId,
                                BinanceSpotSellSourceDto source) {
        if (!uid.equals(source.getUid())
                || !symbol.equals(source.getSymbol())
                || !sellOrderId.equals(source.getSellOrderId())
                || source.getSourceType() == null) {
            return false;
        }
        if (source.getSourceType() == BinanceSpotSellSourceDto.SourceType.TRADE) {
            return source.getSourceTradeId() != null;
        }
        return source.getSourceType() == BinanceSpotSellSourceDto.SourceType.ORDER
                && source.getSourceOrderId() != null;
    }

    private void markSellException(BinanceSpotTradeMatchState sell,
                                   BinanceSpotTradeMatchResult result,
                                   String status) {
        stateMapper.updateMatchProgress(
                sell.getId(), positive(sell.getMatchedQty()), positive(sell.getRemainingQty()), status);
        result.setExceptionSellCount(result.getExceptionSellCount() + 1);
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
        BigDecimal buyFeeAmount = allocateFee(
                buy.getCommission(), buy.getMatchedQty(), matchedQty, buy.getOriginalQty());
        BigDecimal sellFeeAmount = allocateFee(
                sell.getCommission(), sell.getMatchedQty(), matchedQty, sell.getOriginalQty());
        BigDecimal buyFeeQuoteAmount = allocateFee(
                buy.getCommissionQuoteAmount(), buy.getMatchedQty(), matchedQty, buy.getOriginalQty());
        BigDecimal sellFeeQuoteAmount = allocateFee(
                sell.getCommissionQuoteAmount(), sell.getMatchedQty(), matchedQty, sell.getOriginalQty());
        boolean valuationComplete = "COMPLETED".equals(buy.getCommissionValuationStatus())
                && "COMPLETED".equals(sell.getCommissionValuationStatus())
                && buyFeeQuoteAmount != null && sellFeeQuoteAmount != null;
        BigDecimal fee = valuationComplete
                ? buyFeeQuoteAmount.add(sellFeeQuoteAmount) : BigDecimal.ZERO;

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
        match.setFeeRate(BigDecimal.ZERO);
        match.setBuyFeeAsset(buy.getCommissionAsset());
        match.setBuyFeeAmount(buyFeeAmount);
        match.setBuyFeeQuoteAmount(buyFeeQuoteAmount);
        match.setSellFeeAsset(sell.getCommissionAsset());
        match.setSellFeeAmount(sellFeeAmount);
        match.setSellFeeQuoteAmount(sellFeeQuoteAmount);
        match.setFeeValuationComplete(valuationComplete ? 1 : 0);
        match.setPnl(pnl);
        match.setFee(fee);
        match.setNetPnl(valuationComplete ? pnl.subtract(fee) : null);
        return match;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpotActualFeeRebuildResult rebuildWithActualFees(Integer uid, String symbol) {
        validateScope(uid, symbol);
        SpotActualFeeRebuildResult result = commissionValuationService.valueScope(uid, symbol);
        if (result.getFailedCount() > 0) {
            return result;
        }
        result.setAdjustedCorePositionCount(
                stateMapper.adjustFullyLockedCoreQtyForBaseFee(uid, symbol));
        int conflicts = stateMapper.countCoreQtyConflicts(uid, symbol);
        result.setCoreConflictCount(conflicts);
        if (conflicts > 0) {
            return result;
        }
        result.setDeletedMatchCount(matchMapper.deleteByScope(uid, symbol));
        result.setDeletedStateCount(stateMapper.deleteByScope(uid, symbol));
        result.setMatchResult(initializeAndMatch(uid, symbol));
        result.setRebuilt(true);
        return result;
    }

    private BigDecimal allocateFee(BigDecimal totalFee,
                                   BigDecimal previouslyMatchedQty,
                                   BigDecimal matchedQty,
                                   BigDecimal originalQty) {
        if (totalFee == null || originalQty == null || originalQty.signum() <= 0) {
            return null;
        }
        BigDecimal before = totalFee.multiply(positive(previouslyMatchedQty))
                .divide(originalQty, MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal afterQty = positive(previouslyMatchedQty).add(matchedQty);
        BigDecimal after = afterQty.compareTo(originalQty) >= 0
                ? totalFee
                : totalFee.multiply(afterQty)
                .divide(originalQty, MONEY_SCALE, RoundingMode.HALF_UP);
        return after.subtract(before);
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
