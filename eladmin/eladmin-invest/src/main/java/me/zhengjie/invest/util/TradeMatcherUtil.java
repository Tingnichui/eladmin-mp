package me.zhengjie.invest.util;

import me.zhengjie.invest.domain.dto.MatchedTradeInfo;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class TradeMatcherUtil {
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

            Iterator<T> openIt = openList.iterator();
            while (openIt.hasNext()) {
                T open = openIt.next();

                // 跳过数量为 0 的开仓单
                if (qtyGetter.apply(open).compareTo(BigDecimal.ZERO) <= 0) {
                    openIt.remove();
                    continue;
                }

                // 忽略 平仓时间早于开仓时间的
                if (timeGetter.apply(close).before(timeGetter.apply(open))) {
                    continue;
                }

                // 计算撮合数量
                BigDecimal matchQty = qtyGetter.apply(close).min(qtyGetter.apply(open));

                // 创建撮合记录
                MatchedTradeInfo matched = new MatchedTradeInfo(side, feeRate);
                matched.setQty(matchQty);
                matched.setOpenPrice(priceGetter.apply(open));
                matched.setClosePrice(priceGetter.apply(close));

                if (!filter.test(matched)) {
                    continue;
                }

                // 假如撮合列表
                matchedList.add(matched);

                // 更新剩余数量
                qtySetter.accept(open, qtyGetter.apply(open).subtract(matchQty));
                qtySetter.accept(close, qtyGetter.apply(close).subtract(matchQty));

                // 平仓单撮合完则移除
                if (qtyGetter.apply(close).compareTo(BigDecimal.ZERO) <= 0) {
                    closeIt.remove();
                    break;
                }
            }
        }

        return matchedList;
    }

}
