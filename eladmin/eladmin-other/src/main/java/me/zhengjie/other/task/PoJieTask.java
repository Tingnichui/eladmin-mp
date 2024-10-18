package me.zhengjie.other.task;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ColorScheme;
import com.microsoft.playwright.options.Cookie;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.utils.RedisUtils;
import me.zhengjie.utils.enums.RedisKeyEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service("poJieTask")
public class PoJieTask {

    @Resource
    private RedisUtils redisUtils;

    public static void main(String[] args) throws Exception {
        String cookieStr = "";
        new PoJieTask().dailyCheckIn(cookieStr);
    }

    public void dailyCheckIn(String cookieStr) throws Exception {
        if (StringUtils.isBlank(cookieStr)) {
            throw new RuntimeException("cookie不能为空");
        }

        RedisKeyEnum keyEnum = RedisKeyEnum.CHECK_IN_52_PO_JIE_TASK;
        boolean lock = redisUtils.setIfAbsent(keyEnum.getKey(), keyEnum.getDesc(), 10, TimeUnit.MINUTES);
        log.info("定时任务：{}，获取锁：{}", keyEnum.getDesc(), lock);
        if (!lock) return;

        // 随机暂停一会
        TimeUnit.MINUTES.sleep(RandomUtil.randomInt(1, 8));

        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions();
//            launchOptions.setHeadless(false); //取消无头模式，我们才能看见浏览器操作
            launchOptions.setSlowMo(100); //减慢执行速度，以免太快
//            launchOptions.setDevtools(true); //打开浏览器开发者工具，默认不打开
            Browser browser = playwright.chromium().launch(launchOptions);

            Browser.NewContextOptions newContextOptions = new Browser.NewContextOptions();
            newContextOptions.setColorScheme(ColorScheme.DARK); //设置浏览器主题，chromium设置了dark好像没用
            newContextOptions.setViewportSize(1000, 500); //设置浏览器打开后窗口大小
            BrowserContext browserContext = browser.newContext(newContextOptions);

            // 设置cookie
            List<Cookie> cookieList = JSONUtil.toList(cookieStr, Cookie.class);
            if (CollectionUtils.isEmpty(cookieList)) {
                throw new RuntimeException("cookie不能为空");
            }
            browserContext.addCookies(cookieList);


            Page page = browserContext.newPage();
            // 添加初始化脚本来隐藏 webdriver 属性
            String js = "Object.defineProperty(navigator, 'webdriver', {get: () => false});";
            page.addInitScript(js);
            page.navigate("https://www.52pojie.cn");

            // 判断有没有签到元素，有的话进行签到
            Locator checkInBtn = page.locator(String.format("//a[img[@src='%s']]", "https://static.52pojie.cn/static/image/common/qds.png"));
            if (checkInBtn.count() > 0) {
                checkInBtn.first().click();
                // 睡眠一分钟，等待签到
                TimeUnit.MINUTES.sleep(1L);
                return;
            }
            // 判断有没有已经签到的元素，18点之后还没有签到成功，抛出异常
            Locator hasCheckInBtn = page.locator(String.format("//p/img[@src='%s']", "https://static.52pojie.cn/static/image/common/wbs.png"));
            if (hasCheckInBtn.count() == 0 && DateUtil.hour(new Date(), true) > 18) {
                throw new RuntimeException("签到失败");
            }


        } finally {
            redisUtils.del(keyEnum.getKey());
        }

    }


}
