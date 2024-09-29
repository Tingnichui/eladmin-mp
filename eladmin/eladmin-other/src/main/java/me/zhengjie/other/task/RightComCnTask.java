package me.zhengjie.other.task;


import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.utils.RedisUtils;
import me.zhengjie.utils.enums.RedisKeyEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.htmlunit.BrowserVersion;
import org.htmlunit.NicelyResynchronizingAjaxController;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlTableRow;
import org.htmlunit.util.Cookie;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service("rightComCnTask")
public class RightComCnTask {

    @Resource
    private RedisUtils redisUtils;

    public static void main(String[] args) throws Exception {
        String cookieStr = "";
        new RightComCnTask().dailyCheckIn(cookieStr);
    }

    public void dailyCheckIn(String cookieStr) throws Exception {
        if (StringUtils.isBlank(cookieStr)) {
            throw new RuntimeException("cookie不能为空");
        }

        RedisKeyEnum keyEnum = RedisKeyEnum.RIGHT_COM_CN_CHECK_IN_TASK;
        boolean lock = redisUtils.setIfAbsent(keyEnum.getKey(), keyEnum.getDesc(), 10, TimeUnit.MINUTES);
        log.info("定时任务：{}，获取锁：{}", keyEnum.getDesc(), lock);
        if (!lock) return;

        // 随机暂停一会
        Thread.sleep(RandomUtil.randomInt(10, 100) * 1000L);

        try (WebClient webClient = new WebClient(BrowserVersion.FIREFOX)) {
            webClient.getOptions().setCssEnabled(false);//关闭css
            webClient.getOptions().setJavaScriptEnabled(true);//开启js
            webClient.getOptions().setRedirectEnabled(true);//重定向
            webClient.getOptions().setThrowExceptionOnScriptError(false);//关闭js报错
            webClient.getOptions().setTimeout(50000);//超时时间
            webClient.getCookieManager().setCookiesEnabled(true);//允许cookie
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());//设置支持AJAX
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);//关闭404报错

            // 请求地址
            WebRequest webRequest = new WebRequest(new URL("https://www.right.com.cn/forum/home.php?mod=spacecp&ac=credit&op=base"));
            // 添加cookie
            List<Cookie> cookieList = JSON.parseArray(cookieStr, Cookie.class);
            if (CollectionUtils.isEmpty(cookieList)) {
                throw new RuntimeException("cookie不能为空");
            }
            for (Cookie cookie : cookieList) {
                webClient.getCookieManager().addCookie(cookie);
            }

            // 发送请求
            HtmlPage dailyPage = webClient.getPage(webRequest);
            if (dailyPage.asXml().contains("您需要先登录才能继续本操作")) {
                throw new RuntimeException("恩山论坛-cookie失效，请即时处理");
            }
            // 判断今天是否已经签到了
            Date today = new Date();
            String todayStr = DateUtil.format(today, DatePattern.NORM_DATE_PATTERN);
            List<HtmlTableRow> rows = dailyPage.getByXPath(String.format("//tr[td[contains(text(), '%s')] and td[contains(text(), '恩山币')]]", todayStr));

            // 18点之后还没签到抛出异常
            if (CollectionUtils.isEmpty(rows) && DateUtil.hour(today, true) > 18) {
                throw new RuntimeException("恩山论坛-签到失败，请即时处理");
            }

        } finally {
            redisUtils.del(keyEnum.getKey());
        }

    }


}
