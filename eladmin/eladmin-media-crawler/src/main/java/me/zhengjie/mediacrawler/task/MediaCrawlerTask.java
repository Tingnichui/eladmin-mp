package me.zhengjie.mediacrawler.task;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;
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
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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

    public void syncRecentCrawlRecord(String dateStr) {
        Date date = new Date();
        if (StringUtils.isNotBlank(dateStr)) {
            date = DateUtil.parse(dateStr, DatePattern.NORM_DATE_PATTERN);
        }

        // 默认同步三天的数据
        for (int i = 0; i < 3; i++) {
            this.syncCrawlRecord(DateUtil.offsetDay(date, -i));
        }

    }


    public void syncCrawlRecord(Date date) {
        Session session = null;
        try {
            final String logHomeDir = "/home/application/media-crawler-pro/media-crawler-python/logs";
            final String completeLogPath = logHomeDir + "/" + DateUtil.format(date, DatePattern.PURE_DATE_PATTERN);

            // 连接到服务器
            session = JschUtil.getSession(host, 12022, "root", password);

            // 获取当天所有目录文件
            String execCmd = String.format("ls %s", completeLogPath) ;
            String logList = JschUtil.exec(session, execCmd, null);
            if (StringUtils.isBlank(logList)) {
                return;
            }
            // 遍历每个文件
            String[] fileList = logList.split("\n");
            for (String fileName : fileList) {
                // 解析出所有信息
                String[] split = fileName.split("_");
                final String dateStr = split[0];
                final String timeStr = split[1];
                final String platform = split[2];
                final String type = split[3];
                final String keywords = split[4];
                final String recordId = split[split.length - 1].replace(".log", "");
                final DateTime startTime = DateUtil.parse(dateStr + timeStr, DatePattern.PURE_DATETIME_FORMAT);
                final String logCompletePath = completeLogPath + "/" + fileName;

                CrawlerRecord crawlerRecord = crawlerRecordService.getById(recordId);
                if (null == crawlerRecord) {
                    throw new RuntimeException("爬虫记录不存在，及时处理");
                }

                // 进行中的记录处理
                if (!CrawlerRecordStatusEnum.CRAWLING.getCode().equals(crawlerRecord.getCrawlerStatus())) {
                    continue;
                }

                // 需要考虑到爬虫失败重试
                if (null == crawlerRecord.getStartTime()) {
                    crawlerRecord.setStartTime(startTime.toTimestamp());
                }
                // 可能存在多条日志
                if (!crawlerRecord.getLogPath().contains(logCompletePath)){
                    crawlerRecord.setLogPath(crawlerRecord.getLogPath() + "," + logCompletePath);
                }

                // 获取该次爬取最大页数
                {
                    String pageNumCmd = String.format("cat %s | grep 'INFO' | grep 'search' | grep 'keyword' | grep 'page'", logCompletePath);
                    String lastPageStr = JschUtil.exec(session, pageNumCmd, null);
                    if (StringUtils.isBlank(lastPageStr)) {
                        throw new RuntimeException("获取爬取页数出现异常");
                    }

                    String[] pageLogsArray = lastPageStr.split("\n");
                    Integer endPage = getPage(pageLogsArray[pageLogsArray.length - 1], keywords);
                    crawlerRecord.setEndPage(endPage);
                }

                // 判断一下是否顺利完成
                {
                    String tailCmd = String.format("tail -n 20 %s", logCompletePath) ;
                    String tailContentStr = JschUtil.exec(session, tailCmd, null);
                    if (tailContentStr.contains("记录当前爬取的关键词和页码")) {
                        // 标记为异常，下次还要继续处理
                        crawlerRecord.setCrawlerStatus(CrawlerRecordStatusEnum.ERROR.getCode());
                        crawlerRecord.setErrorMsg(tailContentStr);
                    } else if (tailContentStr.contains("Crawler finished")) {
                        crawlerRecord.setCrawlerStatus(CrawlerRecordStatusEnum.FINISH.getCode());
                    }
                }

                crawlerRecordService.updateById(crawlerRecord);

            }


        } catch (Exception e) {
            log.error("同步爬虫记录信息出现异常", e);
            throw e;
        } finally {
            // 关闭连接
            if (null != session) {
                JschUtil.close(session);
            }
        }

    }

    private static Integer getPage(String content, String keywords) {
        String keywordRegex = "search xhs keyword: (.*?), page: (\\d+)";
        List<String> allGroups = ReUtil.getAllGroups(Pattern.compile(keywordRegex), content);
        if (allGroups.size() != 3) {
            throw new RuntimeException("匹配页码出现异常");
        }
        if (!allGroups.get(1).equals(keywords)) {
            throw new RuntimeException("搜索关键词不匹配");
        }
        return Integer.parseInt(allGroups.get(2));
    }


}
