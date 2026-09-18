package me.zhengjie;

import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceTradeInfoExt;
import me.zhengjie.invest.domain.dto.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceCoinFuturesTradeInfoService;
import me.zhengjie.invest.service.BinanceFuturesTradeInfoService;
import me.zhengjie.invest.service.BinanceTradeInfoExtService;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.invest.service.support.BinanceSpotHedgeContext;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BinanceTradeStatsReadOnlyTest {

    private static final Integer TEST_UID = 1014564231;

    @Resource
    private BinanceTradeInfoService binanceTradeInfoService;
    @Resource
    private BinanceFuturesTradeInfoService binanceFuturesTradeInfoService;
    @Resource
    private BinanceCoinFuturesTradeInfoService binanceCoinFuturesTradeInfoService;
    @Resource
    private BinanceAccountInfoService binanceAccountInfoService;
    @Resource
    private BinanceTradeInfoExtService binanceTradeInfoExtService;

    @Test
    void statsShouldNotChangePersistedHedgedQty() {
        Map<Long, String> before = persistedHedgedQty();
        BinanceAccountInfo accountInfo = binanceAccountInfoService.getAccountByUid(TEST_UID);

        BinanceAccountContextHolder.runWith(accountInfo, () -> {
            BinanceSpotHedgeContext hedgeContext = binanceTradeInfoService.createHedgeContext(TEST_UID, "BTCUSDT");
            binanceFuturesTradeInfoService.stats(TEST_UID, hedgeContext);
            binanceCoinFuturesTradeInfoService.stats(TEST_UID, hedgeContext);

            BinanceTradeInfoQueryCriteria criteria = new BinanceTradeInfoQueryCriteria();
            criteria.setUid(TEST_UID);
            criteria.setSymbol("BTCUSDT");
            criteria.setMinProfitPct(new BigDecimal("0.002"));
            binanceTradeInfoService.stats(criteria, hedgeContext);
            binanceTradeInfoService.hedgedStats(hedgeContext);
        });

        assertEquals(before, persistedHedgedQty());
    }

    private Map<Long, String> persistedHedgedQty() {
        return binanceTradeInfoExtService.list().stream().collect(Collectors.toMap(
                BinanceTradeInfoExt::getId,
                ext -> String.valueOf(ext.getHedgedQty())
        ));
    }
}
