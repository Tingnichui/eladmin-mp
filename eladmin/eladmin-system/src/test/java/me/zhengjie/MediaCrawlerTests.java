package me.zhengjie;

import me.zhengjie.mediacrawler.task.MediaCrawlerTask;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MediaCrawlerTests {

    @Resource
    private MediaCrawlerTask mediaCrawlerTask;

    @Test
    void crawl() {
        mediaCrawlerTask.crawl("比基尼");
    }
}

