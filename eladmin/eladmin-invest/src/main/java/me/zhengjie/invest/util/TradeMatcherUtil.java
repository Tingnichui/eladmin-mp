package me.zhengjie.invest.util;

import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class TradeMatcherUtil {

    /**
     * 按成交时间和成交 ID 对现货买卖进行 FIFO 撮合。
     * 已完全撮合的买卖会从传入列表移除，列表中只保留剩余数量。
     */
    public static <T> List<MatchedTradeInfo> matchTradesFifo(
            boolean side,
            String feeRate,
            List<T> openList,
            List<T> closeList,
            Function<T, BigDecimal> qtyGetter,
            BiConsumer<T, BigDecimal> qtySetter,
            Function<T, BigDecimal> priceGetter,
            Function<T, Timestamp> timeGetter,
            Function<T, Long> idGetter
    ) {
        Comparator<T> tradeOrder = Comparator
                .comparing(timeGetter)
                .thenComparing(idGetter, Comparator.nullsLast(Long::compareTo));
        openList.sort(tradeOrder);
        closeList.sort(tradeOrder);

        Deque<T> openQueue = new ArrayDeque<>(openList);
        List<MatchedTradeInfo> matchedList = new ArrayList<>();
        for (T close : closeList) {
            while (qtyGetter.apply(close).compareTo(BigDecimal.ZERO) > 0 && !openQueue.isEmpty()) {
                T open = openQueue.peekFirst();
                if (tradeOrder.compare(open, close) >= 0) {
                    break;
                }

                BigDecimal matchQty = qtyGetter.apply(open).min(qtyGetter.apply(close));
                MatchedTradeInfo matched = new MatchedTradeInfo(side, feeRate);
                matched.setQty(matchQty);
                matched.setOpenPrice(priceGetter.apply(open));
                matched.setClosePrice(priceGetter.apply(close));
                matched.setOpenTime(timeGetter.apply(open));
                matched.setCloseTime(timeGetter.apply(close));
                matchedList.add(matched);

                qtySetter.accept(open, qtyGetter.apply(open).subtract(matchQty));
                qtySetter.accept(close, qtyGetter.apply(close).subtract(matchQty));
                if (qtyGetter.apply(open).compareTo(BigDecimal.ZERO) <= 0) {
                    openQueue.removeFirst();
                }
            }
        }

        openList.clear();
        openList.addAll(openQueue);
        closeList.removeIf(close -> qtyGetter.apply(close).compareTo(BigDecimal.ZERO) <= 0);
        return matchedList;
    }

    /**
     * 通用撮合方法
     *
     * @param side        开仓方向，true做多 false做空
     * @param openList    开仓列表
     * @param closeList   平仓列表
     * @param qtyGetter   获取数量函数
     * @param qtySetter   更新数量函数
     * @param priceGetter 获取价格函数
     * @param feeRate     手续费率
     * @param <T>         交易对象类型（如 BinanceTradeInfo）
     * @return 撮合结果列表
     */
    public static <T> List<MatchedTradeInfo> matchTrades(
            boolean side,
            String feeRate,
            List<T> openList,
            List<T> closeList,
            Function<T, BigDecimal> qtyGetter,
            BiConsumer<T, BigDecimal> qtySetter,
            Function<T, BigDecimal> priceGetter,
            Function<T, Timestamp> timeGetter,
            Predicate<MatchedTradeInfo> filter
    ) {
        List<MatchedTradeInfo> matchedList = new ArrayList<>();

        Iterator<T> closeIt = closeList.iterator();
        while (closeIt.hasNext()) {
            T close = closeIt.next();

            for (int i = 0; i < 2; i++) {

                if (qtyGetter.apply(close).compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                Iterator<T> openIt = openList.iterator();
                while (openIt.hasNext()) {
                    T open = openIt.next();

                    // 忽略 平仓时间早于开仓时间的
                    if (i == 0 && timeGetter.apply(close).before(timeGetter.apply(open))) {
                        continue;
                    }

                    // 计算撮合数量
                    BigDecimal matchQty = qtyGetter.apply(close).min(qtyGetter.apply(open));

                    // 创建撮合记录
                    MatchedTradeInfo matched = new MatchedTradeInfo(side, feeRate);
                    matched.setQty(matchQty);
                    matched.setOpenPrice(priceGetter.apply(open));
                    matched.setClosePrice(priceGetter.apply(close));

                    if (i == 0 && !filter.test(matched)) {
                        continue;
                    }

                    // 假如撮合列表
                    matchedList.add(matched);

                    // 更新剩余数量
                    qtySetter.accept(open, qtyGetter.apply(open).subtract(matchQty));
                    qtySetter.accept(close, qtyGetter.apply(close).subtract(matchQty));

                    // 开仓单 都平仓了就移除
                    if (qtyGetter.apply(open).compareTo(BigDecimal.ZERO) <= 0) {
                        openIt.remove();
                    }

                    // 平仓单撮合完则移除
                    if (qtyGetter.apply(close).compareTo(BigDecimal.ZERO) <= 0) {
                        closeIt.remove();
                        break;
                    }
                }

            }

        }

        return matchedList;
    }


    public static <T> List<MatchedTradeInfo> matchTrades(
            boolean side,
            String feeRate,
            List<T> openList,
            Function<T, BigDecimal> qtyGetter,
            Function<T, BigDecimal> priceGetter,
            BigDecimal currentPrice,
            Consumer<MatchedTradeInfo> customHandler
    ) {
        List<MatchedTradeInfo> matchedList = new ArrayList<>();

        Iterator<T> openIt = openList.iterator();
        while (openIt.hasNext()) {
            T open = openIt.next();
            // 可匹配的仓位数量
            BigDecimal matchQty = qtyGetter.apply(open);
            // 撮合交易记录
            MatchedTradeInfo matched = new MatchedTradeInfo(side, feeRate);
            matched.setQty(matchQty);
            matched.setOpenPrice(priceGetter.apply(open));
            matched.setClosePrice(currentPrice);

            if (customHandler != null) {
                customHandler.accept(matched);
            }

            matchedList.add(matched);
        }
        return matchedList;
    }

}
