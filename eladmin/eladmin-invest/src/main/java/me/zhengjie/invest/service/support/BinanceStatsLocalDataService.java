package me.zhengjie.invest.service.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceCoinFuturesTradeInfo;
import me.zhengjie.invest.domain.BinanceFuturesTradeInfo;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.mapper.BinanceCoinFuturesTradeInfoMapper;
import me.zhengjie.invest.mapper.BinanceFuturesTradeInfoMapper;
import me.zhengjie.invest.mapper.BinanceTradeInfoMapper;
import me.zhengjie.utils.RedisUtils;
import me.zhengjie.utils.enums.OrderDirectionEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BinanceStatsLocalDataService {

    private static final String USD_CLOSE_KEY = "BINANCE:FUTURES:LAST_POS_CLOSE_TIME:";
    private static final String COIN_CLOSE_KEY = "BINANCE:COIN_FUTURES:LAST_POS_CLOSE_TIME:";

    private final BinanceTradeInfoMapper spotMapper;
    private final BinanceFuturesTradeInfoMapper usdFuturesMapper;
    private final BinanceCoinFuturesTradeInfoMapper coinFuturesMapper;
    private final RedisUtils redisUtils;

    @Transactional(readOnly = true)
    public BinanceStatsLocalDataSnapshot load(BinanceTradeInfoQueryCriteria requestCriteria) {
        BinanceStatsLocalDataSnapshot snapshot = new BinanceStatsLocalDataSnapshot();
        long totalStart = System.currentTimeMillis();
        snapshot.setSpotTrades(timed(snapshot, "spot", () -> loadSpotTrades(requestCriteria, snapshot)));
        snapshot.setUsdFuturesTrades(timed(snapshot, "usdFutures",
                () -> loadUsdFuturesTrades(requestCriteria, snapshot)));
        snapshot.setCoinFuturesTrades(timed(snapshot, "coinFutures",
                () -> loadCoinFuturesTrades(requestCriteria, snapshot)));
        snapshot.getQueryElapsedMillis().put("total", elapsed(totalStart));
        return snapshot;
    }

    private List<BinanceTradeInfo> loadSpotTrades(BinanceTradeInfoQueryCriteria source,
                                                  BinanceStatsLocalDataSnapshot snapshot) {
        BinanceTradeInfoQueryCriteria criteria = new BinanceTradeInfoQueryCriteria();
        criteria.setUid(source.getUid());
        criteria.setSymbol(source.getSymbol());
        criteria.setEndTime(source.getEndTime());
        criteria.setOrderColumn("time");
        criteria.setOrderDirection(OrderDirectionEnum.ASC.getValue());
        List<BinanceTradeInfo> trades = spotMapper.findAll(criteria);
        if (trades == null) {
            return new ArrayList<>();
        }
        return trades.stream().filter(trade -> {
            boolean valid = trade != null && trade.getId() != null && trade.getIsBuyer() != null
                    && positive(trade.getQty()) && positive(trade.getPrice()) && trade.getTime() != null;
            if (!valid) {
                addInvalidWarning(snapshot);
            }
            return valid;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    private List<BinanceFuturesTradeInfo> loadUsdFuturesTrades(BinanceTradeInfoQueryCriteria criteria,
                                                               BinanceStatsLocalDataSnapshot snapshot) {
        String symbol = BinanceEnum.SYMBOL.BTCUSDT.name();
        String cacheKey = USD_CLOSE_KEY + criteria.getUid() + ":" + symbol;
        Date cachedClose = criteria.getEndTime() == null ? redisUtils.get(cacheKey, Date.class) : null;
        List<BinanceFuturesTradeInfo> trades = usdFuturesMapper.selectList(
                Wrappers.lambdaQuery(BinanceFuturesTradeInfo.class)
                        .eq(BinanceFuturesTradeInfo::getUid, criteria.getUid())
                        .eq(BinanceFuturesTradeInfo::getSymbol, symbol)
                        .eq(BinanceFuturesTradeInfo::getPositionSide, "SHORT")
                        .gt(cachedClose != null, BinanceFuturesTradeInfo::getTime, cachedClose)
                        .le(criteria.getEndTime() != null, BinanceFuturesTradeInfo::getTime, criteria.getEndTime())
                        .orderByAsc(BinanceFuturesTradeInfo::getTime)
        );
        List<BinanceFuturesTradeInfo> valid = trades == null ? new ArrayList<>() : trades.stream()
                .filter(trade -> {
                    boolean ok = trade != null && trade.getBuyer() != null && positive(trade.getQty())
                            && positive(trade.getPrice()) && trade.getTime() != null;
                    if (!ok) {
                        addInvalidWarning(snapshot);
                    }
                    return ok;
                }).collect(Collectors.toCollection(ArrayList::new));
        Date latestClose = findLatestClose(valid, cachedClose);
        updateCloseCache(criteria.getEndTime(), cacheKey, cachedClose, latestClose);
        return after(valid, latestClose);
    }

    private List<BinanceCoinFuturesTradeInfo> loadCoinFuturesTrades(BinanceTradeInfoQueryCriteria criteria,
                                                                    BinanceStatsLocalDataSnapshot snapshot) {
        String symbol = BinanceEnum.SYMBOL.BTCUSD_PERP.name();
        String cacheKey = COIN_CLOSE_KEY + criteria.getUid() + ":" + symbol;
        Date cachedClose = criteria.getEndTime() == null ? redisUtils.get(cacheKey, Date.class) : null;
        List<BinanceCoinFuturesTradeInfo> trades = coinFuturesMapper.selectList(
                Wrappers.lambdaQuery(BinanceCoinFuturesTradeInfo.class)
                        .eq(BinanceCoinFuturesTradeInfo::getUid, criteria.getUid())
                        .eq(BinanceCoinFuturesTradeInfo::getSymbol, symbol)
                        .eq(BinanceCoinFuturesTradeInfo::getPositionSide, "SHORT")
                        .gt(cachedClose != null, BinanceCoinFuturesTradeInfo::getTime, cachedClose)
                        .le(criteria.getEndTime() != null, BinanceCoinFuturesTradeInfo::getTime, criteria.getEndTime())
                        .orderByAsc(BinanceCoinFuturesTradeInfo::getTime)
        );
        List<BinanceCoinFuturesTradeInfo> valid = trades == null ? new ArrayList<>() : trades.stream()
                .filter(trade -> {
                    boolean ok = trade != null && trade.getBuyer() != null && positive(trade.getQty())
                            && positive(trade.getBaseQty()) && positive(trade.getPrice()) && trade.getTime() != null;
                    if (!ok) {
                        addInvalidWarning(snapshot);
                    }
                    return ok;
                }).collect(Collectors.toCollection(ArrayList::new));
        Date latestClose = findLatestCoinClose(valid, cachedClose);
        snapshot.setCoinPositionStartTime(latestClose == null ? new Date(0L) : latestClose);
        updateCloseCache(criteria.getEndTime(), cacheKey, cachedClose, latestClose);
        return afterCoin(valid, latestClose);
    }

    private Date findLatestClose(List<BinanceFuturesTradeInfo> trades, Date initialClose) {
        BigDecimal position = BigDecimal.ZERO;
        Date latestClose = initialClose;
        for (BinanceFuturesTradeInfo trade : trades) {
            position = trade.getBuyer().equals(1) ? position.subtract(trade.getQty()) : position.add(trade.getQty());
            if (position.compareTo(BigDecimal.ZERO) == 0) {
                latestClose = trade.getTime();
            }
        }
        return latestClose;
    }

    private Date findLatestCoinClose(List<BinanceCoinFuturesTradeInfo> trades, Date initialClose) {
        BigDecimal position = BigDecimal.ZERO;
        Date latestClose = initialClose;
        for (BinanceCoinFuturesTradeInfo trade : trades) {
            position = trade.getBuyer().equals(1) ? position.subtract(trade.getQty()) : position.add(trade.getQty());
            if (position.compareTo(BigDecimal.ZERO) == 0) {
                latestClose = trade.getTime();
            }
        }
        return latestClose;
    }

    private List<BinanceFuturesTradeInfo> after(List<BinanceFuturesTradeInfo> trades, Date closeTime) {
        return trades.stream().filter(trade -> closeTime == null || trade.getTime().after(closeTime))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<BinanceCoinFuturesTradeInfo> afterCoin(List<BinanceCoinFuturesTradeInfo> trades, Date closeTime) {
        return trades.stream().filter(trade -> closeTime == null || trade.getTime().after(closeTime))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void updateCloseCache(Timestamp endTime, String key, Date cachedClose, Date latestClose) {
        if (endTime == null && latestClose != null && (cachedClose == null || latestClose.after(cachedClose))) {
            redisUtils.set(key, latestClose);
        }
    }

    private <T> T timed(BinanceStatsLocalDataSnapshot snapshot, String key, Supplier<T> query) {
        long start = System.currentTimeMillis();
        T result = query.get();
        snapshot.getQueryElapsedMillis().put(key, elapsed(start));
        return result;
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private void addInvalidWarning(BinanceStatsLocalDataSnapshot snapshot) {
        String warning = "部分历史交易数据无效，已忽略";
        if (!snapshot.getWarnings().contains(warning)) {
            snapshot.getWarnings().add(warning);
            log.warn(warning);
        }
    }

    private long elapsed(long start) {
        return Math.max(0L, System.currentTimeMillis() - start);
    }
}
