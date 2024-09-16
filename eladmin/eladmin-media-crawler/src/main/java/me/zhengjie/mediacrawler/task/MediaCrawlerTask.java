package me.zhengjie.mediacrawler.task;

import cn.hutool.extra.ssh.JschUtil;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.mediacrawler.domain.vo.CrawlerTagStats;
import me.zhengjie.mediacrawler.mapper.CrawlerStatsMapper;
import me.zhengjie.utils.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service("mediaCrawlerTask")
public class MediaCrawlerTask {


    @Value("${media.crawler.host}")
    private String host;
    @Value("${media.crawler.password}")
    private String password;


    @Resource
    private CrawlerStatsMapper crawlerStaticMapper;

    public static final String CMD = "sh /home/script/docker_mediacrawlerpro_python.sh " +
            "crawler " +
            "mediacrawlerpro_python " +
            "${PLATFORM} " +
            "${TYPE} " +
            "${KEYWORDS} ";


    public void crawl(String keyword) {
        // 判断一下这个关键词是否已经爬取过
        boolean crawlFlag = crawlerStaticMapper.hasCrawled(keyword);

        // 如果已经爬取过，获取出现频次最高的tag作为搜索关键词
        if (crawlFlag) {
            List<CrawlerTagStats> nextKeyWord = crawlerStaticMapper.getNextKeyWord(keyword, 10);
            if (CollectionUtils.isNotEmpty(nextKeyWord)) {
                keyword = nextKeyWord.get(0).getTag();
            }
        }

        // 目前只测试 小红书
        doCrawl("xhs", "search", keyword);
    }


    private void doCrawl(String platform, String type, String keywords) {
        if (StringUtils.isAnyBlank(platform, type, keywords)) {
            throw new RuntimeException("参数不能为空");
        }
        Session session = null;
        try {
            session = JschUtil.getSession(host, 12022, "root", password);
            String execCmd = CMD
                    .replace("${PLATFORM}", platform)
                    .replace("${TYPE}", type)
                    .replace("${KEYWORDS}", keywords);
            String execResult = JschUtil.exec(session, execCmd, null);
            log.info("执行结果：{}", execResult);

            if (StringUtils.isBlank(execResult) || execResult.contains("Error")) {
                throw new RuntimeException("执行失败，即时处理，" + execResult);
            }
            if (execResult.contains("Warning")) {
                // 存在正在执行的任务，取消掉这次执行
                return;
            }
        } catch (Exception e) {
            log.error("执行失败", e);
            throw e;
        } finally {
            if (null != session) {
                JschUtil.close(session);
            }
        }
    }
}
