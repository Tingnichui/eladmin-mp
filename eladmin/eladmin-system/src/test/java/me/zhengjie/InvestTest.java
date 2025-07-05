package me.zhengjie;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class InvestTest {

    @Resource
    private BinanceTradeInfoService binanceTradeInfoService;

    @Test
    void syncTradeInfo() {
        binanceTradeInfoService.syncTradeInfo(DateUtil.parse("2025-07-04", DatePattern.NORM_DATE_PATTERN));
    }


}
