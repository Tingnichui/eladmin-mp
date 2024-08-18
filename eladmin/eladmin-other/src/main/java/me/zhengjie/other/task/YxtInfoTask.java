package me.zhengjie.other.task;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.other.domain.YxtKunComment;
import me.zhengjie.other.domain.YxtKunDetail;
import me.zhengjie.other.mapper.YxtKunCommentMapper;
import me.zhengjie.other.mapper.YxtKunDetailMapper;
import me.zhengjie.utils.RegexPattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class YxtInfoTask {

    @Resource
    private YxtKunCommentMapper yxtKunCommentMapper;
    @Resource
    private YxtKunDetailMapper yxtKunDetailMapper;

    @Value("${other.yxt.url}")
    private String YXT_URL;

    private String COOKIE;

    public void sync(String cookie) {
        if (StringUtils.isBlank(cookie)) {
            throw new RuntimeException("cookie不能为空");
        }
        this.COOKIE = cookie;
        this.findKun(YXT_URL + "/forum-76-1.html");
    }

    private void findKun(String url) {
        try {
            // 列表请求
            String body = this.doRequest(url);

            // 找出所有a标签
            List<String> list = this.findAllAlabel(body);

            // 找出所有人
            {
                List<String> kunList = list.stream()
                        .filter(v -> v.contains("mod=viewthread"))
                        .map(this::getUrl)
                        .collect(Collectors.toList());
                // 入库
                for (String kun : kunList) {
                    // 获取编号
                    String id = ReUtil.getGroup1("tid=(\\d+)", kun);

                    // 保存详情
                    String kunDetail = doRequest(kun);
                    YxtKunDetail yxtKunDetail = new YxtKunDetail();
                    yxtKunDetail.setKunId(id);
                    yxtKunDetail.setDetail(kunDetail);
                    yxtKunDetailMapper.insert(yxtKunDetail);

                    // 保存评论
                    List<String> allComment = new ArrayList<>();
                    this.findComment(kun, allComment);
                    for (String comment : allComment) {
                        YxtKunComment yxtKunComment = new YxtKunComment();
                        yxtKunComment.setKunId(id);
                        yxtKunComment.setComment(comment);
                        yxtKunCommentMapper.insert(yxtKunComment);
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
