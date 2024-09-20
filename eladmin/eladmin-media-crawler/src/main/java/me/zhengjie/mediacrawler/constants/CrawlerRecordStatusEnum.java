package me.zhengjie.mediacrawler.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum CrawlerRecordStatusEnum {

    INITIAL("1", "未开始"),
    CRAWLING("2", "进行中"),
    ERROR("3", "出现异常"),
    WAIT("4", "等待恢复"),
    FINISH("5", "已完成"),
    ;
    
    public static final List<String> needCrawlStatusList;
    
    static {
        needCrawlStatusList = Arrays.asList(INITIAL.getCode(), CRAWLING.getCode(), WAIT.getCode());
    }

    private final String code;
    private final String mean;

}
