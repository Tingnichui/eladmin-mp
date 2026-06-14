package me.zhengjie.mediacrawler.domain.dto;

import lombok.Data;

@Data
public class CrawlerTagStats {

    private String tag;

    private Long occurrenceCount;
}
