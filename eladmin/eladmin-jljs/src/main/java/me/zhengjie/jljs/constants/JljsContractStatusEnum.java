package me.zhengjie.jljs.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum JljsContractStatusEnum {

    daikaika("1", "待开卡"),
    shiyong("2", "使用中"),
    wancheng("3", "已完成"),
    zanting("4", "暂停"),
    zhongzhi("5", "终止"),
    ;


    private final String code;
    private final String mean;

}
