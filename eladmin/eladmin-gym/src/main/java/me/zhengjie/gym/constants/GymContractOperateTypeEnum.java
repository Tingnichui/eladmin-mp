package me.zhengjie.gym.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum GymContractOperateTypeEnum {

    kaika("1", "开卡"),
    zanting("2", "暂停"),
    tuike("3", "退课"),
    bujiao("4", "补缴"),
    yanqi("5", "延期"),
    ;


    private final String code;
    private final String mean;

}
