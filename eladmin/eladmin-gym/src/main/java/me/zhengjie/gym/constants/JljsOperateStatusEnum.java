package me.zhengjie.gym.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum JljsOperateStatusEnum {

    chenggong("1", "成功"),
    chexiao("2", "已撤销"),
    ;


    private final String code;
    private final String mean;

}
