package me.zhengjie.invest.rest;

import me.zhengjie.exception.BadRequestException;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.dto.BinanceUsdFuturesStatsInfoVO;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceFuturesTradeInfoService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BinanceFuturesTradeInfoControllerTest {

    private final BinanceFuturesTradeInfoService service = mock(BinanceFuturesTradeInfoService.class);
    private final BinanceAccountInfoService accountService = mock(BinanceAccountInfoService.class);
    private final BinanceFuturesTradeInfoController controller =
            new BinanceFuturesTradeInfoController(service, accountService);

    @Test
    void shouldQueryNormalizedUsdFuturesStats() {
        BinanceAccountInfo account = availableAccount(7);
        BinanceUsdFuturesStatsInfoVO stats = new BinanceUsdFuturesStatsInfoVO();
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(service.queryStats(7, "BTCUSDT", "SHORT")).thenReturn(stats);

        ResponseEntity<BinanceUsdFuturesStatsInfoVO> response =
                controller.queryStats(7, " btcusdt ", " short ");

        assertEquals(stats, response.getBody());
        verify(service).queryStats(7, "BTCUSDT", "SHORT");
        assertNull(BinanceAccountContextHolder.get());
    }

    @Test
    void shouldSyncSelectedUsdFuturesSymbol() {
        BinanceAccountInfo account = availableAccount(9);
        when(accountService.getAccountByUid(9)).thenReturn(account);
        when(service.sync(account, "ETHUSDT")).thenReturn(12);

        ResponseEntity<Map<String, Integer>> response = controller.syncSelected(9, "ethusdt");

        assertEquals(Integer.valueOf(12), response.getBody().get("tradeCount"));
        verify(service).sync(account, "ETHUSDT");
        assertNull(BinanceAccountContextHolder.get());
    }

    @Test
    void shouldRejectInvalidPositionSideBeforeCallingService() {
        assertThrows(BadRequestException.class,
                () -> controller.queryStats(7, "BTCUSDT", "SIDEWAYS"));

        verify(service, never()).queryStats(7, "BTCUSDT", "SIDEWAYS");
    }

    private BinanceAccountInfo availableAccount(int uid) {
        BinanceAccountInfo account = new BinanceAccountInfo();
        account.setUid(uid);
        account.setApiValidFlag(1);
        return account;
    }
}
