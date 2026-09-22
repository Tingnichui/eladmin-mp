package me.zhengjie.invest.rest;

import me.zhengjie.exception.BadRequestException;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesStatsInfoVO;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesOrderDto;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesOrderRequest;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceCoinFuturesTradeInfoService;
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

class BinanceCoinFuturesTradeInfoControllerTest {

    private final BinanceCoinFuturesTradeInfoService service = mock(BinanceCoinFuturesTradeInfoService.class);
    private final BinanceAccountInfoService accountService = mock(BinanceAccountInfoService.class);
    private final BinanceCoinFuturesTradeInfoController controller =
            new BinanceCoinFuturesTradeInfoController(service, accountService);

    @Test
    void shouldQueryNormalizedCoinFuturesStats() {
        BinanceAccountInfo account = availableAccount(7);
        BinanceCoinFuturesStatsInfoVO stats = new BinanceCoinFuturesStatsInfoVO();
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(service.queryStats(7, "BTCUSD_PERP", "SHORT")).thenReturn(stats);

        ResponseEntity<BinanceCoinFuturesStatsInfoVO> response =
                controller.queryStats(7, " btcusd_perp ", " short ");

        assertEquals(stats, response.getBody());
        verify(service).queryStats(7, "BTCUSD_PERP", "SHORT");
        assertNull(BinanceAccountContextHolder.get());
    }

    @Test
    void shouldSyncSelectedCoinFuturesSymbol() {
        BinanceAccountInfo account = availableAccount(9);
        when(accountService.getAccountByUid(9)).thenReturn(account);
        when(service.sync(account, "BTCUSD_PERP")).thenReturn(12);

        ResponseEntity<Map<String, Integer>> response = controller.syncSelected(9, "btcusd_perp");

        assertEquals(Integer.valueOf(12), response.getBody().get("tradeCount"));
        verify(service).sync(account, "BTCUSD_PERP");
        assertNull(BinanceAccountContextHolder.get());
    }

    @Test
    void shouldRejectUnsupportedSymbolBeforeCallingService() {
        assertThrows(BadRequestException.class,
                () -> controller.queryStats(7, "ETHUSD_PERP", "SHORT"));

        verify(service, never()).queryStats(7, "ETHUSD_PERP", "SHORT");
    }

    @Test
    void shouldPlaceOrderWithSelectedAccountContext() {
        BinanceAccountInfo account = availableAccount(7);
        BinanceCoinFuturesOrderRequest request = new BinanceCoinFuturesOrderRequest();
        request.setUid(7);
        request.setSymbol(" btcusd_perp ");
        BinanceCoinFuturesOrderDto order = new BinanceCoinFuturesOrderDto();
        order.setOrderId(99L);
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(service.placeOrder(request)).thenAnswer(invocation -> {
            assertEquals(account, BinanceAccountContextHolder.get());
            return order;
        });

        ResponseEntity<BinanceCoinFuturesOrderDto> response = controller.placeOrder(request);

        assertEquals(Long.valueOf(99L), response.getBody().getOrderId());
        assertEquals("BTCUSD_PERP", request.getSymbol());
        assertNull(BinanceAccountContextHolder.get());
    }

    private BinanceAccountInfo availableAccount(int uid) {
        BinanceAccountInfo account = new BinanceAccountInfo();
        account.setUid(uid);
        account.setApiValidFlag(1);
        return account;
    }
}
