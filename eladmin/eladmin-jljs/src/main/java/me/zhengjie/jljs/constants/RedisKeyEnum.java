package me.zhengjie.jljs.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RedisKeyEnum {

    SYNC_ALL_CONTRACT_INFO_TASK("SYNC_ALL_CONTRACT_INFO_TASK", "批量同步合同信息"),
    ;

    private final String key;
    private final String desc;

}
