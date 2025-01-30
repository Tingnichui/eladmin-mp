package me.zhengjie.invest.constants;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 交易状态
 */
@AllArgsConstructor
@Getter
public enum InvestTradeRecordOperateStatusEnum {

    INIT(1, "未创建"),
    CREATED(2, "已创建"),
    OPEN(3, "已建仓"),
    STOP_LOSS(4, "已止损"),
    TAKE_PROFIT(5, "已止盈"),
    ;


    private final Integer status;
    private final String mean;

    public static InvestTradeRecordOperateStatusEnum getByStatus(Integer status) {
        for (InvestTradeRecordOperateStatusEnum statusEnum : InvestTradeRecordOperateStatusEnum.values()) {
            if (statusEnum.getStatus().equals(status)) {
                return statusEnum;
            }
        }
        throw new IllegalArgumentException("未找到对应的交易状态");
    }

}
