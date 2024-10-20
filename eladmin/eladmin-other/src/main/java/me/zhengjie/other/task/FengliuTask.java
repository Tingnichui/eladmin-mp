package me.zhengjie.other.task;

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
import org.htmlunit.util.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service("fengliuTask")
public class FengliuTask {

    @Autowired
    private RedisUtils redisUtils;
    @Value("${other.fengliu.url}")
    private String FENGLIU_URL;

    public static void main(String[] args) throws Exception {
        new FengliuTask().dailyCheckIn("");
    }

    public void dailyCheckIn(String cookieStr) throws Exception {
        if (StringUtils.isBlank(cookieStr)) {
            throw new RuntimeException("cookie不能为空");
        }

        RedisKeyEnum keyEnum = RedisKeyEnum.FENGLIU_DAILY_CHECK_IN_TASK;
        boolean lock = redisUtils.setIfAbsent(keyEnum.getKey(), keyEnum.getDesc(), 10, TimeUnit.MINUTES);
        log.info("定时任务：{}，获取锁：{}", keyEnum.getDesc(), lock);
        if (!lock) return;

        // 随机暂停一会
        TimeUnit.MINUTES.sleep(RandomUtil.randomInt(1, 8));

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
            WebRequest webRequest = new WebRequest(new URL(FENGLIU_URL));
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

            // 登录成功就行了
            boolean contains = dailyPage.asXml().contains("您尚未登录");
            if (contains) {
                throw new RuntimeException("cookie失效，及时处理");
            }

        } finally {
            redisUtils.del(keyEnum.getKey());
        }

    }
}
