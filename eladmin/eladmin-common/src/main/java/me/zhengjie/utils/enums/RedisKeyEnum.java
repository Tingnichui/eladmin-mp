package me.zhengjie.utils.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RedisKeyEnum {

    SYNC_ALL_CONTRACT_INFO_TASK("SYNC_ALL_CONTRACT_INFO_TASK", "批量同步合同信息"),
    SYNC_ALL_YXT_KUN_INFO_TASK("SYNC_ALL_YXT_KUN_INFO_TASK", "批量同步坤坤信息"),
    V2EX_DAILY_CHECK_IN_TASK("V2EX_DAILY_CHECK_IN_TASK", "V2EX每日签到"),
    MEDIA_CRAWLER_TASK("MEDIA_CRAWLER_TASK", "自媒体爬虫"),
    RIGHT_COM_CN_CHECK_IN_TASK("RIGHT_COM_CN_CHECK_IN_TASK", "恩山论坛每日签到"),
    CHECK_IN_52_PO_JIE_TASK("CHECK_IN_52_PO_JIE_TASK", "52破解每日签到"),
    SYNC_GYM_MEMBER_USER_TASK("SYNC_GYM_MEMBER_USER_TASK", "同步健身房会员账户"),
    ;

    private final String key;
    private final String desc;

}
