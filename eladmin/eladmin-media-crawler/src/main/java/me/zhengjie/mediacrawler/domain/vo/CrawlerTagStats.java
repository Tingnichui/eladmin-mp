package me.zhengjie.mediacrawler.domain.vo;

import lombok.Data;

@Data
public class CrawlerTagStats {

    private String tag;

    private Long occurrenceCount;
}
