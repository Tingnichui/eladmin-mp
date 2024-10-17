package me.zhengjie.gym.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 课程类型
 */
@AllArgsConstructor
@Getter
public enum JljsCourseTypeEnum {

    ci("1", "按次消费"),
    tian("2", "按天计时"),
    ;


    private final String code;
    private final String mean;

}
