package me.zhengjie.utils.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ActivityProfileEnum {

    DEV("dev", "开发环境"),
    PROD("prod", "生产环境"),
    ;
    private final String profile;
    private final String desc;

}
