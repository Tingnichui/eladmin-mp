package me.zhengjie.invest.service.support;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.service.BinanceCoinFuturesTradeInfoService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceCoinFuturesUtil;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.invest.util.BinanceUsdFuturesUtil;
import me.zhengjie.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Service
public class BinanceStatsRealtimeService {

    private static final long FRESH_MILLIS = 10_000L;
    private static final long STALE_CACHE_SECONDS = 600L;
    private static final long REQUEST_TIMEOUT_MILLIS = 3_000L;
    private static final int CIRCUIT_FAILURE_THRESHOLD = 3;
    private static final long CIRCUIT_FAILURE_WINDOW_MILLIS = 60_000L;
    private static final long CIRCUIT_OPEN_MILLIS = 30_000L;
    private static final String CACHE_PREFIX = "BINANCE:STATS:REALTIME:";

    private final RedisUtils redisUtils;
    private final BinanceUsdFuturesUtil binanceUsdFuturesUtil;
    private final BinanceCoinFuturesUtil binanceCoinFuturesUtil;
    private final BinanceSpotUtil binanceSpotUtil;
    private final BinanceCoinFuturesTradeInfoService binanceCoinFuturesTradeInfoService;
    private final AsyncTaskExecutor executor;
    private final Map<String, CircuitState> circuits = new ConcurrentHashMap<>();
    private final Map<Integer, CompletableFuture<BinanceStatsRealtimeSnapshot>> inFlightSnapshots = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<BinanceStatsRealtimeSnapshot>> inFlightSpotSnapshots = new ConcurrentHashMap<>();

    public BinanceStatsRealtimeService(RedisUtils redisUtils,
                                       BinanceUsdFuturesUtil binanceUsdFuturesUtil,
                                       BinanceCoinFuturesUtil binanceCoinFuturesUtil,
                                       BinanceSpotUtil binanceSpotUtil,
                                       BinanceCoinFuturesTradeInfoService binanceCoinFuturesTradeInfoService,
                                       @Qualifier("binanceStatsExecutor") AsyncTaskExecutor executor) {
        this.redisUtils = redisUtils;
        this.binanceUsdFuturesUtil = binanceUsdFuturesUtil;
        this.binanceCoinFuturesUtil = binanceCoinFuturesUtil;
        this.binanceSpotUtil = binanceSpotUtil;
        this.binanceCoinFuturesTradeInfoService = binanceCoinFuturesTradeInfoService;
        this.executor = executor;
    }

    public BinanceStatsRealtimeSnapshot load(Integer uid) {
        return load(uid, null);
    }

    public BinanceStatsRealtimeSnapshot load(Integer uid, Date coinPositionStartTime) {
        CompletableFuture<BinanceStatsRealtimeSnapshot> owner = new CompletableFuture<>();
        CompletableFuture<BinanceStatsRealtimeSnapshot> existing = inFlightSnapshots.putIfAbsent(uid, owner);
        if (existing != null) {
            try {
                return existing.get(REQUEST_TIMEOUT_MILLIS + 500L, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.warn("等待合并的交易统计实时请求失败: uid={}, error={}", uid, e.getClass().getSimpleName());
                return unavailableSnapshot("实时数据请求繁忙，请稍后重试");
            }
        }

        try {
            BinanceStatsRealtimeSnapshot snapshot = loadParallel(uid, coinPositionStartTime);
            owner.complete(snapshot);
            return snapshot;
        } catch (RuntimeException e) {
            owner.completeExceptionally(e);
            throw e;
        } finally {
            inFlightSnapshots.remove(uid, owner);
        }
    }

    public BinanceStatsRealtimeSnapshot loadSpot(Integer uid, String symbol) {
        String requestKey = uid + ":" + symbol;
        CompletableFuture<BinanceStatsRealtimeSnapshot> owner = new CompletableFuture<>();
        CompletableFuture<BinanceStatsRealtimeSnapshot> existing = inFlightSpotSnapshots.putIfAbsent(requestKey, owner);
        if (existing != null) {
            try {
                return existing.get(REQUEST_TIMEOUT_MILLIS + 500L, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.warn("等待合并的现货统计实时请求失败: uid={}, symbol={}, error={}",
                        uid, symbol, e.getClass().getSimpleName());
                return unavailableSnapshot("实时数据请求繁忙，请稍后重试", "currentSpotPrice");
            }
        }

        try {
            BinanceStatsRealtimeSnapshot snapshot = loadSpotParallel(uid, symbol);
            owner.complete(snapshot);
            return snapshot;
        } catch (RuntimeException e) {
            owner.completeExceptionally(e);
            throw e;
        } finally {
            inFlightSpotSnapshots.remove(requestKey, owner);
        }
    }

    private BinanceStatsRealtimeSnapshot loadParallel(Integer uid, Date coinPositionStartTime) {
        BinanceAccountInfo accountInfo = BinanceAccountContextHolder.get();
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(REQUEST_TIMEOUT_MILLIS);
        PendingItem<BigDecimal> usdPrice = submit(
                accountInfo, uid, "usdFuturesPrice", CACHE_PREFIX + "PRICE:USD:BTCUSDT", "U本位价格",
                () -> binanceUsdFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSDT), this::toBigDecimal
        );
        PendingItem<BigDecimal> coinPrice = submit(
                accountInfo, uid, "coinFuturesPrice", CACHE_PREFIX + "PRICE:COIN:BTCUSD_PERP", "币本位价格",
                () -> binanceCoinFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSD_PERP), this::toBigDecimal
        );
        PendingItem<BigDecimal> fundingFee = submit(
                accountInfo, uid, "coinFundingFee", CACHE_PREFIX + "FUNDING:COIN:" + uid, "币本位资金费",
                () -> coinPositionStartTime == null
                        ? binanceCoinFuturesTradeInfoService.calculatePositionFundingFee(uid, BinanceEnum.SYMBOL.BTCUSD_PERP)
                        : binanceCoinFuturesTradeInfoService.calculatePositionFundingFee(
                                uid, BinanceEnum.SYMBOL.BTCUSD_PERP, coinPositionStartTime),
                this::toBigDecimal
        );
        ItemResult<BigDecimal> usdResult = await(usdPrice, deadlineNanos);
        ItemResult<BigDecimal> coinResult = await(coinPrice, deadlineNanos);
        ItemResult<BigDecimal> fundingResult = await(fundingFee, deadlineNanos);

        BinanceStatsRealtimeSnapshot snapshot = new BinanceStatsRealtimeSnapshot();
        snapshot.setUsdFuturesPrice(apply(snapshot, usdPrice.statusKey, usdResult));
        snapshot.setCoinFuturesPrice(apply(snapshot, coinPrice.statusKey, coinResult));
        snapshot.setCoinFundingFee(apply(snapshot, fundingFee.statusKey, fundingResult));
        return snapshot;
    }

    private BinanceStatsRealtimeSnapshot loadSpotParallel(Integer uid, String symbol) {
        BinanceAccountInfo accountInfo = BinanceAccountContextHolder.get();
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(REQUEST_TIMEOUT_MILLIS);
        PendingItem<BigDecimal> spotPrice = submit(
                accountInfo, uid, "currentSpotPrice", CACHE_PREFIX + "PRICE:SPOT:" + symbol, "现货价格",
                () -> binanceSpotUtil.getPrice(BinanceEnum.SYMBOL.valueOf(symbol)), this::toBigDecimal
        );
        ItemResult<BigDecimal> spotPriceResult = await(spotPrice, deadlineNanos);

        BinanceStatsRealtimeSnapshot snapshot = new BinanceStatsRealtimeSnapshot();
        snapshot.setCurrentSpotPrice(apply(snapshot, spotPrice.statusKey, spotPriceResult));
        return snapshot;
    }

    private <T> PendingItem<T> submit(BinanceAccountInfo accountInfo,
                                      Integer uid,
                                      String statusKey,
                                      String cacheKey,
                                      String label,
                                      Supplier<T> loader,
                                      Function<Object, T> converter) {
        AtomicBoolean failureRecorded = new AtomicBoolean(false);
        String circuitKey = uid + ":" + statusKey;
        Future<ItemResult<T>> future;
        try {
            future = executor.submit(() -> withAccount(accountInfo,
                    () -> loadItem(circuitKey, cacheKey, label, loader, converter, failureRecorded)));
        } catch (RuntimeException e) {
            recordFailure(circuitKey, failureRecorded);
            log.warn("交易统计线程池繁忙: item={}, error={}", label, e.getClass().getSimpleName());
            ItemResult<T> result = fallback(getCachedValue(cacheKey, label), converter,
                    label, "请求繁忙", 0L);
            future = CompletableFuture.completedFuture(result);
        }
        return new PendingItem<>(statusKey, circuitKey, cacheKey, label, converter, failureRecorded, future);
    }

    private <T> ItemResult<T> loadItem(String circuitKey,
                                       String cacheKey,
                                       String label,
                                       Supplier<T> loader,
                                       Function<Object, T> converter,
                                       AtomicBoolean failureRecorded) {
        long start = System.currentTimeMillis();
        CachedValue cached = getCachedValue(cacheKey, label);
        Date now = new Date();
        if (isFresh(cached, now)) {
            T cachedValue = convertCachedValue(cached, converter, label);
            if (cachedValue != null) {
                return ItemResult.live(cachedValue, cached.getUpdatedAt(), elapsed(start));
            }
            cached = null;
        }

        CircuitState circuit = circuits.computeIfAbsent(circuitKey, key -> new CircuitState());
        if (circuit.isOpen(now.getTime())) {
            return fallback(cached, converter, label, "请求暂时熔断", elapsed(start));
        }

        try {
            T value = loader.get();
            Date updatedAt = new Date();
            redisUtils.set(cacheKey, new CachedValue(value, updatedAt), STALE_CACHE_SECONDS);
            circuit.recordSuccess();
            return ItemResult.live(value, updatedAt, elapsed(start));
        } catch (Exception e) {
            recordFailure(circuitKey, failureRecorded);
            log.warn("交易统计实时数据获取失败: item={}, error={}", label, e.getClass().getSimpleName());
            return fallback(cached, converter, label, "获取失败", elapsed(start));
        }
    }

    private <T> ItemResult<T> await(PendingItem<T> pending, long deadlineNanos) {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) {
            return timeout(pending);
        }
        try {
            return pending.future.get(remainingNanos, TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            return timeout(pending);
        } catch (Exception e) {
            log.warn("交易统计并行任务失败: item={}, error={}", pending.label, e.getClass().getSimpleName());
            recordFailure(pending.circuitKey, pending.failureRecorded);
            return fallback(getCachedValue(pending.cacheKey, pending.label), pending.converter,
                    pending.label, "获取失败", REQUEST_TIMEOUT_MILLIS);
        }
    }

    private <T> ItemResult<T> timeout(PendingItem<T> pending) {
        pending.future.cancel(true);
        recordFailure(pending.circuitKey, pending.failureRecorded);
        log.warn("交易统计实时数据请求超时: item={}", pending.label);
        return fallback(getCachedValue(pending.cacheKey, pending.label), pending.converter,
                pending.label, "请求超时", REQUEST_TIMEOUT_MILLIS);
    }

    private <T> ItemResult<T> fallback(CachedValue cached,
                                       Function<Object, T> converter,
                                       String label,
                                       String reason,
                                       long elapsedMillis) {
        if (cached != null && cached.getValue() != null) {
            T cachedValue = convertCachedValue(cached, converter, label);
            if (cachedValue != null) {
                String message = label + reason + "，当前使用缓存数据";
                return ItemResult.cache(cachedValue, cached.getUpdatedAt(), message, elapsedMillis);
            }
        }
        return ItemResult.unavailable(label + reason + "，暂不可用", elapsedMillis);
    }

    private <T> T apply(BinanceStatsRealtimeSnapshot snapshot, String statusKey, ItemResult<T> result) {
        snapshot.getStatuses().put(statusKey, new BinanceStatsRealtimeSnapshot.RealtimeStatus(
                result.status, result.updatedAt, result.message, result.elapsedMillis
        ));
        if (result.message != null) {
            snapshot.getWarnings().add(result.message);
        }
        return result.value;
    }

    private void recordFailure(String circuitKey, AtomicBoolean failureRecorded) {
        if (failureRecorded.compareAndSet(false, true)) {
            circuits.computeIfAbsent(circuitKey, key -> new CircuitState()).recordFailure(System.currentTimeMillis());
        }
    }

    private boolean isFresh(CachedValue cached, Date now) {
        return cached != null && cached.getUpdatedAt() != null
                && now.getTime() - cached.getUpdatedAt().getTime() <= FRESH_MILLIS;
    }

    private <T> T withAccount(BinanceAccountInfo accountInfo, Callable<T> callable) throws Exception {
        try {
            BinanceAccountContextHolder.set(accountInfo);
            return callable.call();
        } finally {
            BinanceAccountContextHolder.clear();
        }
    }

    private BinanceStatsRealtimeSnapshot unavailableSnapshot(String message) {
        return unavailableSnapshot(message,
                "usdFuturesPrice", "coinFuturesPrice", "coinFundingFee");
    }

    private BinanceStatsRealtimeSnapshot unavailableSnapshot(String message, String... keys) {
        BinanceStatsRealtimeSnapshot snapshot = new BinanceStatsRealtimeSnapshot();
        snapshot.getWarnings().add(message);
        for (String key : keys) {
            snapshot.getStatuses().put(key,
                    new BinanceStatsRealtimeSnapshot.RealtimeStatus("UNAVAILABLE", null, message, 0L));
        }
        return snapshot;
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

    private long elapsed(long start) {
        return Math.max(0L, System.currentTimeMillis() - start);
    }

    private static class PendingItem<T> {
        private final String statusKey;
        private final String circuitKey;
        private final String cacheKey;
        private final String label;
        private final Function<Object, T> converter;
        private final AtomicBoolean failureRecorded;
        private final Future<ItemResult<T>> future;

        private PendingItem(String statusKey, String circuitKey, String cacheKey, String label,
                            Function<Object, T> converter, AtomicBoolean failureRecorded,
                            Future<ItemResult<T>> future) {
            this.statusKey = statusKey;
            this.circuitKey = circuitKey;
            this.cacheKey = cacheKey;
            this.label = label;
            this.converter = converter;
            this.failureRecorded = failureRecorded;
            this.future = future;
        }
    }

    private static class ItemResult<T> {
        private final T value;
        private final String status;
        private final Date updatedAt;
        private final String message;
        private final long elapsedMillis;

        private ItemResult(T value, String status, Date updatedAt, String message, long elapsedMillis) {
            this.value = value;
            this.status = status;
            this.updatedAt = updatedAt;
            this.message = message;
            this.elapsedMillis = elapsedMillis;
        }

        private static <T> ItemResult<T> live(T value, Date updatedAt, long elapsedMillis) {
            return new ItemResult<>(value, "LIVE", updatedAt, null, elapsedMillis);
        }

        private static <T> ItemResult<T> cache(T value, Date updatedAt, String message, long elapsedMillis) {
            return new ItemResult<>(value, "CACHE", updatedAt, message, elapsedMillis);
        }

        private static <T> ItemResult<T> unavailable(String message, long elapsedMillis) {
            return new ItemResult<>(null, "UNAVAILABLE", null, message, elapsedMillis);
        }
    }

    private class CircuitState {
        private int failureCount;
        private long firstFailureAt;
        private long openUntil;

        private synchronized boolean isOpen(long now) {
            if (openUntil > now) {
                return true;
            }
            if (openUntil > 0) {
                recordSuccess();
            }
            return false;
        }

        private synchronized void recordFailure(long now) {
            if (firstFailureAt == 0 || now - firstFailureAt > CIRCUIT_FAILURE_WINDOW_MILLIS) {
                firstFailureAt = now;
                failureCount = 1;
            } else {
                failureCount++;
            }
            if (failureCount >= CIRCUIT_FAILURE_THRESHOLD) {
                openUntil = now + CIRCUIT_OPEN_MILLIS;
            }
        }

        private synchronized void recordSuccess() {
            failureCount = 0;
            firstFailureAt = 0;
            openUntil = 0;
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
