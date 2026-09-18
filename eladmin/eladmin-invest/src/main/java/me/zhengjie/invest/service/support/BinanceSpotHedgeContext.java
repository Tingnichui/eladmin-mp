package me.zhengjie.invest.service.support;

import me.zhengjie.invest.domain.BinanceTradeInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 单次统计请求内的现货对冲分配状态，不写入数据库。
 */
public class BinanceSpotHedgeContext {

    private final List<BinanceTradeInfo> spotInfos;
    private final Map<Long, BinanceTradeInfo> spotInfoById;

    public BinanceSpotHedgeContext(List<BinanceTradeInfo> spotInfos) {
        this.spotInfos = spotInfos == null ? Collections.emptyList() : spotInfos;
        this.spotInfos.forEach(spot -> spot.setHedgedQty(BigDecimal.ZERO));
        this.spotInfos.sort(Comparator.comparing(BinanceTradeInfo::getPrice));
        this.spotInfoById = this.spotInfos.stream()
                .collect(Collectors.toMap(BinanceTradeInfo::getId, Function.identity()));
    }

    public HedgeResult allocate(BigDecimal lowPrice, BigDecimal highPrice, BigDecimal qty, Integer limit) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            return HedgeResult.empty();
        }

        List<BinanceTradeInfo> candidates = findCandidates(lowPrice, highPrice, limit);
        BigDecimal availableQty = candidates.stream()
                .map(BinanceTradeInfo::getNetQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (availableQty.compareTo(qty) < 0 && highPrice != null) {
            candidates = findCandidates(lowPrice, null, limit);
        }

        BigDecimal remainingQty = qty;
        BigDecimal hedgedQty = BigDecimal.ZERO;
        BigDecimal hedgedAmount = BigDecimal.ZERO;
        for (BinanceTradeInfo spot : candidates) {
            BigDecimal matchQty = spot.getNetQty().min(remainingQty);
            if (matchQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            spot.setHedgedQty(spot.getHedgedQty().add(matchQty));
            hedgedQty = hedgedQty.add(matchQty);
            hedgedAmount = hedgedAmount.add(spot.getPrice().multiply(matchQty));
            remainingQty = remainingQty.subtract(matchQty);
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }
        return new HedgeResult(hedgedQty, hedgedAmount);
    }

    public BigDecimal getHedgedQty(Long tradeId) {
        BinanceTradeInfo spotInfo = spotInfoById.get(tradeId);
        return spotInfo == null ? BigDecimal.ZERO : spotInfo.getHedgedQty();
    }

    public List<BinanceTradeInfo> getHedgedTrades() {
        return spotInfos.stream()
                .filter(spot -> spot.getHedgedQty().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    private List<BinanceTradeInfo> findCandidates(BigDecimal lowPrice, BigDecimal highPrice, Integer limit) {
        return spotInfos.stream()
                .filter(spot -> spot.getNetQty().compareTo(BigDecimal.ZERO) > 0)
                .filter(spot -> lowPrice == null || spot.getPrice().compareTo(lowPrice) >= 0)
                .filter(spot -> highPrice == null || spot.getPrice().compareTo(highPrice) <= 0)
                .limit(limit == null ? Long.MAX_VALUE : limit.longValue())
                .collect(Collectors.toList());
    }

    public static class HedgeResult {

        private final BigDecimal qty;
        private final BigDecimal amount;

        private HedgeResult(BigDecimal qty, BigDecimal amount) {
            this.qty = qty;
            this.amount = amount;
        }

        private static HedgeResult empty() {
            return new HedgeResult(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        public BigDecimal getQty() {
            return qty;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public BigDecimal getAveragePrice() {
            return qty.compareTo(BigDecimal.ZERO) > 0
                    ? amount.divide(qty, 8, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }
    }
}
