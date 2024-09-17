package me.zhengjie.mediacrawler.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrawlerCookiesAccountStatusEnum {

    INVALID(-1, "无效"),
    VALID(0, "有效"),
    ;

    private final Integer code;
    private final String mean;

}
