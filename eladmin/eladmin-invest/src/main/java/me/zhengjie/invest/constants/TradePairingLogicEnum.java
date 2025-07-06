package me.zhengjie.invest.constants;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 交易状态
 */
@AllArgsConstructor
@Getter
public enum TradePairingLogicEnum {

    MAX_PROFIT("MAX_PROFIT", "最大收益（低买高卖）"),
    FIFO("FIFO", "真实成交（先买先卖）"),
    ;


    private final String key;
    private final String mean;

    public static TradePairingLogicEnum getByKey(String status) {
        for (TradePairingLogicEnum statusEnum : TradePairingLogicEnum.values()) {
            if (statusEnum.getKey().equals(status)) {
                return statusEnum;
            }
        }
        throw new IllegalArgumentException("未找到对应的撮合方式");
    }

}
