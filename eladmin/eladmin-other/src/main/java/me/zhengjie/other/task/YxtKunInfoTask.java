package me.zhengjie.other.task;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.other.domain.YxtKunComment;
import me.zhengjie.other.domain.YxtKunDetail;
import me.zhengjie.other.mapper.YxtKunCommentMapper;
import me.zhengjie.other.mapper.YxtKunDetailMapper;
import me.zhengjie.utils.RedisUtils;
import me.zhengjie.utils.RegexPattern;
import me.zhengjie.utils.enums.RedisKeyEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service("yxtKunInfoTask")
public class YxtKunInfoTask {

    @Resource
    private YxtKunCommentMapper yxtKunCommentMapper;
    @Resource
    private YxtKunDetailMapper yxtKunDetailMapper;
    @Autowired
    private RedisUtils redisUtils;
    @Value("${other.yxt.url}")
    private String YXT_URL;

    private String COOKIE;

    private static final String source = "1";

    public void syncAll(String cookie) {
        RedisKeyEnum keyEnum = RedisKeyEnum.SYNC_ALL_YXT_KUN_INFO_TASK;
        try {
            if (StringUtils.isBlank(cookie)) {
                throw new RuntimeException("cookie不能为空");
            }

            boolean lock = redisUtils.setIfAbsent(keyEnum.getKey(), keyEnum.getDesc(), 8, TimeUnit.HOURS);
            log.info("定时任务：{}，获取锁：{}", keyEnum.getDesc(), lock);
            if (!lock) return;

            this.COOKIE = cookie;
            this.findKun(YXT_URL + "/forum-76-1.html");
        } catch (RuntimeException e) {
            log.error("定时任务：{}，发生异常：{}", keyEnum.getDesc(), e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            redisUtils.del(keyEnum.getKey());
        }
    }

    public void updateInfo() {
        while (true) {
            List<YxtKunDetail> yxtKunDetailList = yxtKunDetailMapper.selectList(
                    Wrappers.lambdaQuery(YxtKunDetail.class)
                            .isNull(YxtKunDetail::getNickName)
                            .last("limit 1000")
            );
            if (CollectionUtils.isEmpty(yxtKunDetailList)) {
                break;
            }
            for (YxtKunDetail yxtKunDetail : yxtKunDetailList) {
                this.parseAndSaveInfo(yxtKunDetail.getDetail(), yxtKunDetail);
            }
        }

        while (true) {
            List<YxtKunComment> yxtKunCommentList = yxtKunCommentMapper.selectList(
                    Wrappers.lambdaQuery(YxtKunComment.class)
                            .isNull(YxtKunComment::getCommentTime)
                            .last("limit 1000")
            );
            if (CollectionUtils.isEmpty(yxtKunCommentList)) {
                break;
            }
            for (YxtKunComment yxtKunComment : yxtKunCommentList) {
                this.parseAndSaveCommnet(yxtKunComment.getComment(), yxtKunComment);
                yxtKunCommentMapper.deleteById(yxtKunComment.getId());

            }
        }



    }

    private void findKun(String url) {
        try {
            // 列表请求
            String body = this.doRequest(url);

            // 找出所有a标签
            List<String> list = this.findAllAlabel(body);

            // 找出所有人
            {
                List<String> infoUrlList = list.stream()
                        .filter(v -> v.contains("mod=viewthread"))
                        .map(this::getUrl)
                        .collect(Collectors.toList());
                // 入库
                for (String infoUrl : infoUrlList) {
                    // 获取编号
                    String id = ReUtil.getGroup1("tid=(\\d+)", infoUrl);

                    // 保存详情
                    String infoHtmlStr = this.doRequest(infoUrl);
                    YxtKunDetail yxtKunDetail = new YxtKunDetail();
                    yxtKunDetail.setKunId(id);
                    yxtKunDetail.setInfoUrl(infoUrl);
                    yxtKunDetail.setSource(source);
                    this.parseAndSaveInfo(infoHtmlStr, yxtKunDetail);
                    Integer yxtKunDetailId = yxtKunDetail.getId();

                    // 保存评论
                    List<String> allCommentHtmlStrList = new ArrayList<>();
                    this.findComment(infoUrl, allCommentHtmlStrList);
                    for (String commentHtmlStr : allCommentHtmlStrList) {
                        YxtKunComment yxtKunComment = new YxtKunComment();
                        yxtKunComment.setKunDetailId(yxtKunDetailId);
                        yxtKunComment.setKunId(id);
                        this.parseAndSaveCommnet(commentHtmlStr, yxtKunComment);
                    }
                }
            }

            // 找下一页
            {
                String next = list.stream().filter(v -> v.contains("下一页")).findFirst().orElse(null);
                String nextUrl = getUrl(next);
                // 为空就停止
                if (StringUtils.isBlank(nextUrl)) {
                    return;
                }
                // 递归处理
                this.findKun(nextUrl);
            }

        } catch (Exception e) {
            log.error("同步异常", e);
            throw e;
        }
    }

    private void parseAndSaveInfo(String htmlStr, YxtKunDetail yxtKunDetail) {
        Document document = Jsoup.parse(htmlStr);
        // 提取昵称
        yxtKunDetail.setNickName(extractText(document, "div.nex_model_terms_name"));
        // 提取服务内容
        yxtKunDetail.setDetail(extractText(document, "li:has(h2:contains(服务内容)) div.nex_text_desc"));
        // 提取费用详情
        yxtKunDetail.setExpenses(extractText(document, "li:has(h2:contains(费用详情)) div.nex_text_desc"));
        // 提取联系地址
        yxtKunDetail.setAddress(extractText(document, "li:has(h2:contains(联系地址)) div.nex_text_desc"));
        // 提取联系方式
        Element contactInfoEle = document.selectFirst("div.nex_reply_contant");
        if (null != contactInfoEle) {
            contactInfoEle.select("div.locked").remove();
            contactInfoEle.select("script").remove();
            yxtKunDetail.setContactInfo(contactInfoEle.text());
        }
        if (null != yxtKunDetail.getId()) {
            yxtKunDetailMapper.updateById(yxtKunDetail);
        } else {
            yxtKunDetailMapper.insert(yxtKunDetail);
        }
    }

    private void parseAndSaveCommnet(String html,YxtKunComment yxtKunComment) {
        Elements elements = Jsoup.parse(html).select("div.nex_vt_post_box");
        if (CollectionUtils.isEmpty(elements)) {
            return;
        }

        for (Element element : elements) {
            // 评论时间
            String commentTime = extractText(element, "div.nex_vt_replyothers_topintel > i");
            if (StringUtils.isNotBlank(commentTime) && commentTime.length() == 19) {
                yxtKunComment.setCommentTime(DateUtil.parse(commentTime, DatePattern.NORM_DATETIME_PATTERN).toTimestamp());
            }
            // 评论内容
            yxtKunComment.setComment(extractText(element, "span.nex_reply_contant"));

            // 评论不为空就保存到数据库
            if (StringUtils.isNotBlank(yxtKunComment.getComment())) {
                yxtKunComment.setId(null);
                yxtKunCommentMapper.insert(yxtKunComment);
            }
        }
    }

    private static String extractText(Element document, String cssQuery) {
        Element element = document.selectFirst(cssQuery);
        if (element != null) {
            return element.text();
        }
        return null;
    }

    private void findComment(String url, List<String> allComment) {
        String commentDetail = doRequest(url);
        // 解析评论入库
        allComment.add(commentDetail);

        // 处理下一页评论
        String nextLabel = findAllAlabel(commentDetail).stream().filter(v -> v.contains("下一页")).findFirst().orElse(null);
        String nextUrl = getUrl(nextLabel);
        // 为空就停止
        if (StringUtils.isNotBlank(nextUrl)) {
            // 递归处理
            findComment(nextUrl, allComment);
        }

    }

    private List<String> findAllAlabel(String body) {
        Pattern compile = Pattern.compile(RegexPattern.A_TAG_REG);
        List<String> list = ReUtil.findAll(compile, body, 0);
        return list;
    }

    private String doRequest(String url) {
        try {
            // 随机暂停一会
            Thread.sleep(RandomUtil.randomInt(2 * 1000, 5 * 1000));

            // 开始请求
            HttpRequest request = HttpUtil.createGet(url, false);
            request.header("Host", "www.yxt51.com");
            request.header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1");
            request.cookie(COOKIE);
            HttpResponse httpResponse = request.execute();
            String body = httpResponse.body();
            // 移除所有的制表符、回车符和换行符
            body = body.replaceAll(RegexPattern.BLANK_EXCEPT_SPACE_REG, "");
            // 去掉标签之间的空格
            body = body.replaceAll(RegexPattern.WHITE_SPACE_IN_HTML_TAG, "><");
            // 去除掉注释内容
            body = body.replaceAll(RegexPattern.HTML_NOTES_REG, "");
            return body;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String getUrl(String v) {
        if (StringUtils.isBlank(v)) {
            return null;
        }
        // 找出跳转url
        String rowJumpUrl = ReUtil.getGroup1(RegexPattern.HREF_REG, v);
        if (rowJumpUrl.contains("javascript")) {
            return null;
        }
        // url转码
        String jumpUrl = decodeUrl(rowJumpUrl);
        // 拼接完整url
        if (jumpUrl.startsWith("http")) {
            return jumpUrl;
        } else {
            return YXT_URL + (jumpUrl.startsWith("/") ? jumpUrl : "/" + jumpUrl);
        }
    }

    private String decodeUrl(String url) {
        String decode = URLUtil.decode(url);
        String s = decode.replaceAll("&amp;", "&");
        return s;
    }

}
