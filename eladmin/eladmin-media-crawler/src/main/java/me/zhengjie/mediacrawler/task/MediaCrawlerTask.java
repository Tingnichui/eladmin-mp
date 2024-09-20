package me.zhengjie.mediacrawler.task;

import cn.hutool.extra.ssh.JschUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.mediacrawler.constants.CrawlerCookiesAccountStatusEnum;
import me.zhengjie.mediacrawler.constants.CrawlerRecordStatusEnum;
import me.zhengjie.mediacrawler.domain.CrawlerCookiesAccount;
import me.zhengjie.mediacrawler.domain.CrawlerRecord;
import me.zhengjie.mediacrawler.domain.vo.CrawlerTagStats;
import me.zhengjie.mediacrawler.mapper.CrawlerStatsMapper;
import me.zhengjie.mediacrawler.service.CrawlerCookiesAccountService;
import me.zhengjie.mediacrawler.service.CrawlerRecordService;
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
    @Resource
    private CrawlerRecordService crawlerRecordService;

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
            CrawlerRecord crawlerRecord = new CrawlerRecord();
            crawlerRecord.setPlatform("xhs");
            crawlerRecord.setCrawlerType("search");
            crawlerRecord.setKeywords(keyword);
            crawlerRecord.setStartPage(1);
            crawlerRecord.setCrawlerStatus(CrawlerRecordStatusEnum.INITIAL.getCode());
            crawlerRecordService.save(crawlerRecord);

            // 执行爬虫
            this.doCrawl(crawlerRecord.getId());

        } finally {
            redisUtils.del(keyEnum.getKey());
        }

    }


    /**
     * 执行爬虫
     * @param crawlerRecordId 爬虫记录主键id
     */
    public void doCrawl(Integer crawlerRecordId) {
        if (null == crawlerRecordId) {
            return;
        }
        CrawlerRecord crawlerRecord = crawlerRecordService.getById(crawlerRecordId);
        if (null == crawlerRecord) {
            return;
        }

        // 校验状态
        if (!CrawlerRecordStatusEnum.needCrawlStatusList.contains(crawlerRecord.getCrawlerStatus())) {
            return;
        }

        // 校验参数
        final String platform = crawlerRecord.getPlatform();
        final String crawlerType = crawlerRecord.getCrawlerType();
        final String keywords = crawlerRecord.getKeywords();
        final Integer startPage = crawlerRecord.getStartPage();
        if (StringUtils.isAnyBlank(platform, crawlerType, keywords)) {
            throw new RuntimeException("参数不能为空");
        }

        // 连接服务器进行操作
        Session session = null;
        try {
            session = JschUtil.getSession(host, 12022, "root", password);
            String execCmd = CMD
                    .replace("${PLATFORM}", platform)
                    .replace("${TYPE}", crawlerType)
                    .replace("${KEYWORDS}", keywords)
                    .replace("${RECORD_ID}", crawlerRecordId.toString())
                    .replace("${START_PAGE}", null != startPage ? startPage.toString() : "1");
            String execResult = JschUtil.exec(session, execCmd, null);
            log.info("执行结果：{}", execResult);

            if (StringUtils.isBlank(execResult) || execResult.contains("Error")) {
                throw new RuntimeException("执行失败，即时处理，" + execResult);
            }
            if (execResult.contains("Warning")) {
                // 存在正在执行的任务，取消掉这次执行
                return;
            }
            // 更新爬虫状态
            crawlerRecordService.update(
                    Wrappers.lambdaUpdate(CrawlerRecord.class)
                            .eq(CrawlerRecord::getId,crawlerRecordId)
                            .set(CrawlerRecord::getCrawlerStatus, CrawlerRecordStatusEnum.CRAWLING.getCode())
            );

        } catch (Exception e) {
            log.error("执行失败", e);
            // 更新爬虫状态与错误信息
            crawlerRecordService.update(
                    Wrappers.lambdaUpdate(CrawlerRecord.class)
                            .eq(CrawlerRecord::getId,crawlerRecordId)
                            .set(CrawlerRecord::getCrawlerStatus, CrawlerRecordStatusEnum.ERROR.getCode())
                            .set(CrawlerRecord::getErrorMsg, e.getMessage())
            );
        } finally {
            if (null != session) {
                JschUtil.close(session);
            }
        }
    }

    /**
     * 检查失效账号
     */
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
