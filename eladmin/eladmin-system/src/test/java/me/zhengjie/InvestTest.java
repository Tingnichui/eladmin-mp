package me.zhengjie;

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
        binanceTradeInfoService.syncTradeInfo("BTCUSDT");
    }


}
