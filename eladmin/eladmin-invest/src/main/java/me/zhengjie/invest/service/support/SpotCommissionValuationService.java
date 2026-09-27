package me.zhengjie.invest.service.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.InvestKlinesRecord;
import me.zhengjie.invest.domain.dto.SpotActualFeeRebuildResult;
import me.zhengjie.invest.mapper.BinanceTradeInfoMapper;
import me.zhengjie.invest.util.BinanceSpotUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保留币安原始手续费币种，并按成交时市场价格折算为交易对计价资产。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpotCommissionValuationService {

    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final long MINUTE_MILLIS = 60_000L;

    private final BinanceSpotUtil binanceSpotUtil;
    private final BinanceTradeInfoMapper binanceTradeInfoMapper;
    private final Map<String, BigDecimal> minutePriceCache = new ConcurrentHashMap<>();

    public void valueAll(Collection<BinanceTradeInfo> trades) {
        if (trades == null) {
            return;
        }
        trades.forEach(this::value);
    }

    public SpotActualFeeRebuildResult valueScope(Integer uid, String symbol) {
        List<BinanceTradeInfo> trades = binanceTradeInfoMapper.findScopeForFeeValuation(uid, symbol);
        SpotActualFeeRebuildResult result = new SpotActualFeeRebuildResult();
        result.setTradeCount(trades.size());
        for (BinanceTradeInfo trade : trades) {
            value(trade);
            binanceTradeInfoMapper.updateById(trade);
            if (STATUS_COMPLETED.equals(trade.getCommissionValuationStatus())) {
                result.setValuedCount(result.getValuedCount() + 1);
            } else {
                result.setFailedCount(result.getFailedCount() + 1);
            }
        }
        return result;
    }

    public void value(BinanceTradeInfo trade) {
        if (trade == null || trade.getCommission() == null || trade.getCommissionAsset() == null) {
            markFailed(trade);
            return;
        }
        try {
            String quoteAsset = quoteAsset(trade.getSymbol());
            String baseAsset = baseAsset(trade.getSymbol(), quoteAsset);
            String commissionAsset = trade.getCommissionAsset().toUpperCase(Locale.ROOT);
            trade.setCommissionQuoteAsset(quoteAsset);
            BigDecimal quotePrice;
            String source;
            if (commissionAsset.equals(quoteAsset) || trade.getCommission().signum() == 0) {
                quotePrice = ONE;
                source = "QUOTE_ASSET";
            } else if (commissionAsset.equals(baseAsset)) {
                quotePrice = requirePositive(trade.getPrice(), "成交价格");
                source = "TRADE_PRICE";
            } else {
                quotePrice = historicalMinutePrice(commissionAsset + quoteAsset, trade);
                source = "KLINE_1M";
            }
            trade.setCommissionQuotePrice(quotePrice);
            trade.setCommissionQuoteAmount(trade.getCommission().multiply(quotePrice)
                    .setScale(16, RoundingMode.HALF_UP));
            trade.setCommissionValuationSource(source);
            trade.setCommissionValuationStatus(STATUS_COMPLETED);
        } catch (Exception e) {
            markFailed(trade);
            log.warn("现货手续费估值失败: tradeId={}, symbol={}, asset={}, error={}",
                    trade.getId(), trade.getSymbol(), trade.getCommissionAsset(), e.getClass().getSimpleName());
        }
    }

    private BigDecimal historicalMinutePrice(String conversionSymbol, BinanceTradeInfo trade) {
        if (trade.getTime() == null) {
            throw new IllegalArgumentException("成交时间为空");
        }
        long minuteStart = trade.getTime().getTime() / MINUTE_MILLIS * MINUTE_MILLIS;
        String cacheKey = conversionSymbol + ":" + minuteStart;
        return minutePriceCache.computeIfAbsent(cacheKey, key -> {
            BinanceEnum.SYMBOL symbol = BinanceEnum.SYMBOL.valueOf(conversionSymbol);
            List<InvestKlinesRecord> klines = binanceSpotUtil.getKlines(
                    symbol, "1m", minuteStart, minuteStart + MINUTE_MILLIS - 1);
            if (klines == null || klines.isEmpty()) {
                throw new IllegalStateException("历史 K 线为空");
            }
            InvestKlinesRecord kline = klines.get(0);
            if (kline.getVolume() != null && kline.getVolume().signum() > 0
                    && kline.getTurnover() != null && kline.getTurnover().signum() > 0) {
                return kline.getTurnover().divide(kline.getVolume(), 16, RoundingMode.HALF_UP);
            }
            return requirePositive(kline.getClosePrice(), "历史收盘价");
        });
    }

    private String quoteAsset(String symbol) {
        String normalized = symbol == null ? "" : symbol.toUpperCase(Locale.ROOT);
        for (String quote : new String[]{"USDT", "USDC", "BUSD"}) {
            if (normalized.endsWith(quote)) {
                return quote;
            }
        }
        throw new IllegalArgumentException("不支持的计价资产");
    }

    private String baseAsset(String symbol, String quoteAsset) {
        return symbol.toUpperCase(Locale.ROOT).substring(0, symbol.length() - quoteAsset.length());
    }

    private BigDecimal requirePositive(BigDecimal value, String name) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(name + "无效");
        }
        return value;
    }

    private void markFailed(BinanceTradeInfo trade) {
        if (trade == null) {
            return;
        }
        trade.setCommissionQuotePrice(null);
        trade.setCommissionQuoteAmount(null);
        trade.setCommissionValuationSource(null);
        trade.setCommissionValuationStatus(STATUS_FAILED);
    }
}
