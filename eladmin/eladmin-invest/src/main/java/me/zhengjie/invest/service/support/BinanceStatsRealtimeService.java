package me.zhengjie.invest.service.support;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.service.BinanceCoinFuturesTradeInfoService;
import me.zhengjie.invest.util.BinanceCoinFuturesUtil;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.invest.util.BinanceUsdFuturesUtil;
import me.zhengjie.utils.RedisUtils;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class BinanceStatsRealtimeService {

    private static final long FRESH_MILLIS = 10_000L;
    private static final long STALE_CACHE_SECONDS = 600L;
    private static final String CACHE_PREFIX = "BINANCE:STATS:REALTIME:";

    private final RedisUtils redisUtils;
    private final BinanceUsdFuturesUtil binanceUsdFuturesUtil;
    private final BinanceCoinFuturesUtil binanceCoinFuturesUtil;
    private final BinanceSpotUtil binanceSpotUtil;
    private final BinanceCoinFuturesTradeInfoService binanceCoinFuturesTradeInfoService;

    public BinanceStatsRealtimeSnapshot load(Integer uid) {
        BinanceStatsRealtimeSnapshot snapshot = new BinanceStatsRealtimeSnapshot();

        snapshot.setUsdFuturesPrice(load(
                snapshot,
                "usdFuturesPrice",
                CACHE_PREFIX + "PRICE:USD:BTCUSDT",
                "U本位价格",
                () -> binanceUsdFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSDT),
                this::toBigDecimal
        ));
        snapshot.setCoinFuturesPrice(load(
                snapshot,
                "coinFuturesPrice",
                CACHE_PREFIX + "PRICE:COIN:BTCUSD_PERP",
                "币本位价格",
                () -> binanceCoinFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSD_PERP),
                this::toBigDecimal
        ));
        snapshot.setCoinFundingFee(load(
                snapshot,
                "coinFundingFee",
                CACHE_PREFIX + "FUNDING:COIN:" + uid,
                "币本位资金费",
                () -> binanceCoinFuturesTradeInfoService.calculatePositionFundingFee(uid, BinanceEnum.SYMBOL.BTCUSD_PERP),
                this::toBigDecimal
        ));
        snapshot.setAccountInfo(load(
                snapshot,
                "accountInfo",
                CACHE_PREFIX + "ACCOUNT:" + uid,
                "账户资产",
                () -> binanceSpotUtil.usdStats(true),
                value -> value
        ));

        return snapshot;
    }

    private <T> T load(BinanceStatsRealtimeSnapshot snapshot,
                       String statusKey,
                       String cacheKey,
                       String label,
                       Supplier<T> loader,
                       Function<Object, T> converter) {
        CachedValue cached = getCachedValue(cacheKey, label);
        Date now = new Date();
        if (cached != null && cached.getUpdatedAt() != null
                && now.getTime() - cached.getUpdatedAt().getTime() <= FRESH_MILLIS) {
            T cachedValue = convertCachedValue(cached, converter, label);
            if (cachedValue != null) {
                snapshot.getStatuses().put(statusKey,
                        new BinanceStatsRealtimeSnapshot.RealtimeStatus("LIVE", cached.getUpdatedAt(), null));
                return cachedValue;
            }
            cached = null;
        }

        try {
            T value = loader.get();
            CachedValue current = new CachedValue(value, now);
            redisUtils.set(cacheKey, current, STALE_CACHE_SECONDS);
            snapshot.getStatuses().put(statusKey,
                    new BinanceStatsRealtimeSnapshot.RealtimeStatus("LIVE", now, null));
            return value;
        } catch (Exception e) {
            log.warn("交易统计实时数据获取失败: item={}, error={}", label, e.getClass().getSimpleName());
            if (cached != null && cached.getValue() != null) {
                T cachedValue = convertCachedValue(cached, converter, label);
                if (cachedValue != null) {
                    String message = label + "获取失败，当前使用缓存数据";
                    snapshot.getStatuses().put(statusKey,
                            new BinanceStatsRealtimeSnapshot.RealtimeStatus("CACHE", cached.getUpdatedAt(), message));
                    snapshot.getWarnings().add(message);
                    return cachedValue;
                }
            }

            String message = label + "暂不可用";
            snapshot.getStatuses().put(statusKey,
                    new BinanceStatsRealtimeSnapshot.RealtimeStatus("UNAVAILABLE", null, message));
            snapshot.getWarnings().add(message);
            return null;
        }
    }

    private CachedValue getCachedValue(String key, String label) {
        try {
            return redisUtils.get(key, CachedValue.class);
        } catch (Exception e) {
            log.warn("交易统计缓存读取失败: item={}, error={}", label, e.getClass().getSimpleName());
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        return value == null ? null : new BigDecimal(String.valueOf(value));
    }

    private <T> T convertCachedValue(CachedValue cached, Function<Object, T> converter, String label) {
        try {
            return converter.apply(cached.getValue());
        } catch (Exception e) {
            log.warn("交易统计缓存格式无效: item={}, error={}", label, e.getClass().getSimpleName());
            return null;
        }
    }

    @Data
    public static class CachedValue implements Serializable {
        private Object value;
        private Date updatedAt;

        public CachedValue() {
        }

        public CachedValue(Object value, Date updatedAt) {
            this.value = value;
            this.updatedAt = updatedAt;
        }
    }
}
