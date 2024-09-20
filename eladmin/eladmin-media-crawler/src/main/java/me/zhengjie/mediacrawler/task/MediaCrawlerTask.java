package me.zhengjie.mediacrawler.task;

import cn.hutool.extra.ssh.JschUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.mediacrawler.constants.CrawlerCookiesAccountStatusEnum;
import me.zhengjie.mediacrawler.domain.CrawlerCookiesAccount;
import me.zhengjie.mediacrawler.domain.vo.CrawlerTagStats;
import me.zhengjie.mediacrawler.mapper.CrawlerStatsMapper;
import me.zhengjie.mediacrawler.service.CrawlerCookiesAccountService;
import me.zhengjie.utils.RedisUtils;
import me.zhengjie.utils.StringUtils;
import me.zhengjie.utils.enums.RedisKeyEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service("mediaCrawlerTask")
public class MediaCrawlerTask {


    @Value("${media.crawler.host}")
    private String host;
    @Value("${media.crawler.password}")
    private String password;


    @Resource
    private CrawlerStatsMapper crawlerStaticMapper;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private CrawlerCookiesAccountService crawlerCookiesAccountService;

    public static final String CMD = "sh /home/script/docker_mediacrawlerpro_python.sh " +
            "crawler " +
            "mediacrawlerpro_python " +
            "${PLATFORM} " +
            "${TYPE} " +
            "${KEYWORDS} " +
            "${RECORD_ID} " +
            "${START_PAGE} "
            ;


    public void crawl(String keyword) {
        // 加锁
        RedisKeyEnum keyEnum = RedisKeyEnum.MEDIA_CRAWLER_TASK;
        boolean lock = redisUtils.setIfAbsent(keyEnum.getKey(), keyEnum.getDesc(), 10, TimeUnit.MINUTES);
        log.info("定时任务：{}，获取锁：{}", keyEnum.getDesc(), lock);
        if (!lock) return;

        try {
            // 判断一下这个关键词是否已经爬取过
            boolean crawlFlag = crawlerStaticMapper.hasCrawled(keyword);

            // 如果已经爬取过，获取出现频次最高的tag作为搜索关键词
            if (crawlFlag) {
                List<CrawlerTagStats> nextKeyWord = crawlerStaticMapper.getTagListByKeyWord(keyword, 5, 10);
                if (CollectionUtils.isNotEmpty(nextKeyWord)) {
                    keyword = nextKeyWord.get(0).getTag();
                }
            }

            // 目前只测试 小红书
            this.doCrawl("xhs", "search", keyword, null, null);

        } finally {
            redisUtils.del(keyEnum.getKey());
        }

    }


    private void doCrawl(String platform, String type, String keywords, Integer recordId, Integer startPage) {
        if (StringUtils.isAnyBlank(platform, type, keywords) || null == recordId || null == startPage) {
            throw new RuntimeException("参数不能为空");
        }
        Session session = null;
        try {
            session = JschUtil.getSession(host, 12022, "root", password);
            String execCmd = CMD
                    .replace("${PLATFORM}", platform)
                    .replace("${TYPE}", type)
                    .replace("${KEYWORDS}", keywords)
                    .replace("${RECORD_ID}", recordId.toString())
                    .replace("${START_PAGE}", startPage.toString());
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

    public void checkAccountValidStatus() {
        long count = crawlerCookiesAccountService.count(
                Wrappers.lambdaQuery(CrawlerCookiesAccount.class)
                        .eq(CrawlerCookiesAccount::getStatus, CrawlerCookiesAccountStatusEnum.INVALID.getCode())
        );
        if (count > 0) {
            {
                throw new RuntimeException("存在无效的账号，请及时处理");
            }
        }
    }
}
