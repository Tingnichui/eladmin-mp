package me.zhengjie.invest.service.support;

import lombok.RequiredArgsConstructor;
import me.zhengjie.invest.domain.dto.BinanceSpotSellSourceDto;
import me.zhengjie.utils.RedisUtils;
import org.springframework.stereotype.Component;

/**
 * 快捷卖出现货订单的 Redis 来源存储。
 */
@Component
@RequiredArgsConstructor
public class BinanceSpotSellSourceStore {

    private static final String KEY_PREFIX = "BINANCE:SPOT:SELL_SOURCE:";

    private final RedisUtils redisUtils;

    public BinanceSpotSellSourceDto find(Integer uid, String symbol, Long sellOrderId) {
        if (uid == null || symbol == null || sellOrderId == null) {
            return null;
        }
        return redisUtils.get(key(uid, symbol, sellOrderId), BinanceSpotSellSourceDto.class);
    }

    private String key(Integer uid, String symbol, Long sellOrderId) {
        return KEY_PREFIX + uid + ":" + symbol + ":" + sellOrderId;
    }
}
