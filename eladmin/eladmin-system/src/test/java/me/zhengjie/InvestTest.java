package me.zhengjie;

import me.zhengjie.invest.domain.dto.BinanceOrderVO;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceCoinFuturesTradeInfoService;
import me.zhengjie.invest.service.BinanceFuturesTradeInfoService;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class InvestTest {

    private static final Integer TEST_UID = 1014564231;

    @Resource
    private BinanceTradeInfoService binanceTradeInfoService;
    @Resource
    private BinanceFuturesTradeInfoService binanceFuturesTradeInfoService;
    @Resource
    private BinanceCoinFuturesTradeInfoService binanceCoinFuturesTradeInfoService;
    @Resource
    private BinanceAccountInfoService binanceAccountInfoService;

    @Test
    void syncTradeInfo() {
        binanceTradeInfoService.syncTradeInfo("BTCUSDT");
    }

    @Test
    void stats() {
        binanceFuturesTradeInfoService.stats(TEST_UID);
    }

    @Test
    void lastPosCloseTime() {
        assertNotNull(binanceFuturesTradeInfoService.getLastPosCloseTime(TEST_UID));
        assertNotNull(binanceCoinFuturesTradeInfoService.getLastPosCloseTime(TEST_UID));
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
