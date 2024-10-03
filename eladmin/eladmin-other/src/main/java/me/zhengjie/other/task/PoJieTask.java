package me.zhengjie.other.task;


import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.utils.RedisUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.htmlunit.*;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlImage;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.util.Cookie;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.URL;
import java.util.List;

@Slf4j
@Service("poJieTask")
public class PoJieTask {

    @Resource
    private RedisUtils redisUtils;
    // https://blog.csdn.net/paq6411/article/details/140814778

    public static void main(String[] args) throws Exception {
        String cookieStr = "";
        new PoJieTask().dailyCheckIn(cookieStr);
    }

    public void dailyCheckIn(String cookieStr) throws Exception {
        if (StringUtils.isBlank(cookieStr)) {
            throw new RuntimeException("cookie不能为空");
        }

//        RedisKeyEnum keyEnum = RedisKeyEnum.CHECK_IN_52_PO_JIE_TASK;
//        boolean lock = redisUtils.setIfAbsent(keyEnum.getKey(), keyEnum.getDesc(), 10, TimeUnit.MINUTES);
//        log.info("定时任务：{}，获取锁：{}", keyEnum.getDesc(), lock);
//        if (!lock) return;

        // 随机暂停一会
//        TimeUnit.MINUTES.sleep(RandomUtil.randomInt(1, 8));

        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            webClient.getOptions().setCssEnabled(true);//关闭css
            webClient.getOptions().setJavaScriptEnabled(true);//开启js
            webClient.getOptions().setRedirectEnabled(true);//重定向
            webClient.getOptions().setThrowExceptionOnScriptError(true);//关闭js报错
            webClient.getOptions().setTimeout(50000);//超时时间
            webClient.getCookieManager().setCookiesEnabled(true);//允许cookie
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());//设置支持AJAX
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);//关闭404报错

            // 添加cookie
            List<Cookie> cookieList = JSON.parseArray(cookieStr, Cookie.class);
            if (CollectionUtils.isEmpty(cookieList)) {
                throw new RuntimeException("cookie不能为空");
            }
            for (Cookie cookie : cookieList) {
                webClient.getCookieManager().addCookie(cookie);
            }

            // 发送请求
            WebRequest webRequest = new WebRequest(new URL("https://www.52pojie.cn"));
            HtmlPage homePage = webClient.getPage(webRequest);
            // 判断有没有签到元素，有的话进行签到
            HtmlAnchor cheackIn = homePage.getFirstByXPath(String.format("//a[img[@src='%s']]", "https://static.52pojie.cn/static/image/common/qds.png"));
            if (cheackIn != null) {
                webClient.waitForBackgroundJavaScript(10 * 1000); // 等待 JavaScript 执行完成
                HtmlPage clickPage = cheackIn.click();
                webClient.waitForBackgroundJavaScript(10 * 1000); // 等待 JavaScript 执行完成
                String redirectedUrl = clickPage.getUrl().toString();
                System.err.println(redirectedUrl);
                return;
            }
            // 判断有没有已经签到的元素，没有加抛出异常
            HtmlImage hasCheck = homePage.getFirstByXPath(String.format("//p/img[@src='%s']", "https://static.52pojie.cn/static/image/common/wbs.png"));
            if (hasCheck == null) {
                throw new RuntimeException("签到失败");
            }
        } finally {
//            redisUtils.del(keyEnum.getKey());
        }

    }


}
