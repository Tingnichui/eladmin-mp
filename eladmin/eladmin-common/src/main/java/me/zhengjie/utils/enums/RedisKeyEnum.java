package me.zhengjie.utils.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RedisKeyEnum {

    SYNC_ALL_CONTRACT_INFO_TASK("SYNC_ALL_CONTRACT_INFO_TASK", "批量同步合同信息"),
    SYNC_ALL_YXT_KUN_INFO_TASK("SYNC_ALL_YXT_KUN_INFO_TASK", "批量同步坤坤信息"),
    ;

    private final String key;
    private final String desc;

}
