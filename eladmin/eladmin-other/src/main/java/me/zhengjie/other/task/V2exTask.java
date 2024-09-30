package me.zhengjie.other.task;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.utils.RedisUtils;
import me.zhengjie.utils.enums.RedisKeyEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.htmlunit.*;
import org.htmlunit.html.HtmlDivision;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSpan;
import org.htmlunit.util.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service("v2exTask")
public class V2exTask {

    @Value("${proxy.host}")
    private String proxyHost;
    @Value("${proxy.port}")
    private Integer proxyPort;
    @Resource
    private RedisUtils redisUtils;

    public static void main(String[] args) throws Exception {
        new V2exTask().dailyCheckIn("[]");
    }

    public void dailyCheckIn(String cookieStr) throws Exception {
        if (StringUtils.isBlank(cookieStr)) {
            throw new RuntimeException("cookie不能为空");
        }

        // 生产环境不支持
//        LinkedHashSet<String> localIpSet = NetUtil.localIpv4s();
//        if (localIpSet.contains("172.20.213.52")) {
//            return;
//        }

        RedisKeyEnum keyEnum = RedisKeyEnum.V2EX_DAILY_CHECK_IN_TASK;
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

            // 设置代理
            ProxyConfig proxyConfig = webClient.getOptions().getProxyConfig();
            proxyConfig.setProxyHost(proxyHost);
            proxyConfig.setProxyPort(proxyPort);

            // 请求地址
            WebRequest webRequest = new WebRequest(new URL("https://www.v2ex.com/mission/daily"));
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
            // 判断是否cookie是否失效
            HtmlDivision divElement = dailyPage.getFirstByXPath("//div[@class='message']");
            if (null != divElement) {
                throw new RuntimeException("v2ex-cookie失效，请即时处理");
            }
            // 判断今天是否已经签到了
            HtmlSpan beforeCheckInMsg = dailyPage.getFirstByXPath("//span[@class='gray' and contains(text(),'每日登录奖励已领取')]");
            if (null != beforeCheckInMsg) {
                log.info("今天已经签到过了");
                return;
            }

            // 找到签到元素
            HtmlInput signInDocument = dailyPage.getFirstByXPath("//input[@value='领取 X 铜币']");
            if (null == signInDocument) {
                throw new RuntimeException("找不到签到元素，请即时处理");
            }
            // 点击签到
            HtmlPage clickResultPage = signInDocument.click();
            // 再次请求一下签到页面之后找一下是否签到成功，如果没找说明签到失败了
            HtmlSpan checkInMsg = clickResultPage.getFirstByXPath("//span[@class='gray' and contains(text(),'每日登录奖励已领取')]");
            if (null == checkInMsg) {
                log.info("签到成功后跳转页面：{}", clickResultPage.asXml());
                throw new RuntimeException("签到失败，请即时处理");
            }
        } finally {
            redisUtils.del(keyEnum.getKey());
        }

    }
}
