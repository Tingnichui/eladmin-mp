package me.zhengjie.utils.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DatasourceEnum {

    MASTER("master", "默认数据库"),
    MEDIA_CRAWLER("media_crawler", "媒体爬虫"),
    ;

    private final String source;
    private final String desc;
}
