package me.zhengjie.invest.rest;

import me.zhengjie.exception.BadRequestException;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceCoinFuturesTradeInfoService;
import me.zhengjie.invest.service.BinanceFuturesTradeInfoService;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceSpotUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BinanceTradeInfoControllerTest {

    private final BinanceTradeInfoService spotService = mock(BinanceTradeInfoService.class);
    private final BinanceFuturesTradeInfoService usdFuturesService = mock(BinanceFuturesTradeInfoService.class);
    private final BinanceCoinFuturesTradeInfoService coinFuturesService =
            mock(BinanceCoinFuturesTradeInfoService.class);
    private final BinanceAccountInfoService accountService = mock(BinanceAccountInfoService.class);
    private final BinanceSpotUtil spotUtil = mock(BinanceSpotUtil.class);
    private final BinanceTradeInfoController controller = new BinanceTradeInfoController();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "binanceTradeInfoService", spotService);
        ReflectionTestUtils.setField(controller, "binanceFuturesTradeInfoService", usdFuturesService);
        ReflectionTestUtils.setField(controller, "binanceCoinFuturesTradeInfoService", coinFuturesService);
        ReflectionTestUtils.setField(controller, "binanceAccountInfoService", accountService);
        ReflectionTestUtils.setField(controller, "binanceSpotUtil", spotUtil);
    }

    @Test
    void shouldSyncOnlySelectedAccountAndSpotSymbol() {
        BinanceAccountInfo account = new BinanceAccountInfo();
        account.setUid(7);
        account.setApiValidFlag(1);
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(spotService.syncTradeInfo(account, "BTCUSDT")).thenReturn(2);
        when(usdFuturesService.sync(account)).thenReturn(1);
        when(coinFuturesService.sync(account)).thenReturn(0);

        ResponseEntity<Map<String, Object>> response = controller.syncSelected(7, "BTCUSDT");

        assertEquals(2, response.getBody().get("spotCount"));
        assertEquals(1, response.getBody().get("usdFuturesCount"));
        assertEquals(0, response.getBody().get("coinFuturesCount"));
        assertEquals(true, response.getBody().get("accountUpdated"));
        verify(spotService).syncTradeInfo(account, "BTCUSDT");
        verify(usdFuturesService).sync(account);
        verify(coinFuturesService).sync(account);
        verify(spotUtil).usdStats(false);
        assertNull(BinanceAccountContextHolder.get());
    }

    @Test
    void shouldRejectNonSpotSymbolBeforeLoadingAccount() {
        assertThrows(BadRequestException.class,
                () -> controller.syncSelected(7, "BTCUSD_PERP"));

        verify(accountService, never()).getAccountByUid(7);
        verifyNoInteractions(spotService);
    }
}
