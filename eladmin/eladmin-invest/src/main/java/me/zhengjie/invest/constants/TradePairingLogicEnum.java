package me.zhengjie.invest.constants;


import lombok.AllArgsConstructor;
import lombok.Getter;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;

import java.math.BigDecimal;

/**
 * 交易状态
 */
@AllArgsConstructor
@Getter
public enum TradePairingLogicEnum {

    MAX_PROFIT("MAX_PROFIT", "最大收益（低买高卖）") {
        @Override
        public boolean allowMatch(MatchedTradeInfo matchedTradeInfo) {
            return true;
        }
    },
    FIFO("FIFO", "真实成交（先买先卖）") {
        @Override
        public boolean allowMatch(MatchedTradeInfo matchedTradeInfo) {
            // 收益必须大于0.3%
            return matchedTradeInfo.getBuyTime().before(matchedTradeInfo.getSellTime()) &&
                    matchedTradeInfo.profitRate().compareTo(new BigDecimal("0.003")) > 0;
        }
    },
    ;


    private final String key;
    private final String mean;
    public abstract boolean allowMatch(MatchedTradeInfo matchedTradeInfo);


    public static TradePairingLogicEnum getByKey(String status) {
        for (TradePairingLogicEnum statusEnum : TradePairingLogicEnum.values()) {
            if (statusEnum.getKey().equals(status)) {
                return statusEnum;
            }
        }
        throw new IllegalArgumentException("未找到对应的撮合方式");
    }

}
