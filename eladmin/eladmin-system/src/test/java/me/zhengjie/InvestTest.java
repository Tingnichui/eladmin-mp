package me.zhengjie;

import me.zhengjie.invest.domain.dto.BinanceOrderVO;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceFuturesTradeInfoService;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class InvestTest {

    @Resource
    private BinanceTradeInfoService binanceTradeInfoService;
    @Resource
    private BinanceFuturesTradeInfoService binanceFuturesTradeInfoService;
    @Resource
    private BinanceAccountInfoService binanceAccountInfoService;

    @Test
    void syncTradeInfo() {
        binanceTradeInfoService.syncTradeInfo("BTCUSDT");
    }

    @Test
    void stats() {
        binanceFuturesTradeInfoService.stats();
    }

    @Test
    void createPos() {
        BinanceOrderVO posInfo = new BinanceOrderVO();
        posInfo.setPosDir(true);
        posInfo.setPosQty(new BigDecimal("0.0001"));
        posInfo.setOpenPrice(new BigDecimal("85600"));

        BinanceAccountContextHolder.runWith(binanceAccountInfoService.getAccountByIdCardName("耿辉"), () -> {
            binanceTradeInfoService.createPos(posInfo);
        });
    }
}
