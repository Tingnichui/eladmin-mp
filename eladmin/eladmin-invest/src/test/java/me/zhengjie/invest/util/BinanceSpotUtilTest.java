package me.zhengjie.invest.util;

import cn.hutool.http.Method;
import me.zhengjie.invest.domain.dto.BinanceOrderApiDto;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BinanceSpotUtilTest {

    @Test
    void shouldReconcileUncertainOrderByGeneratedClientOrderId() {
        BinanceHttpClient httpClient = mock(BinanceHttpClient.class);
        BinanceSpotUtil spotUtil = new BinanceSpotUtil();
        ReflectionTestUtils.setField(spotUtil, "apiHost", "https://api.binance.com");
        ReflectionTestUtils.setField(spotUtil, "binanceHttpClient", httpClient);
        AtomicReference<String> generatedClientOrderId = new AtomicReference<>();

        when(httpClient.request(anyString(), anyString(), any(),
                anyBoolean(), any(Method.class))).thenAnswer(invocation -> {
            Map<String, Object> params = invocation.getArgument(2);
            Method method = invocation.getArgument(4);
            if (Method.POST == method) {
                String clientOrderId = String.valueOf(params.get("newClientOrderId"));
                generatedClientOrderId.set(clientOrderId);
                throw new RuntimeException("order response lost");
            }
            assertEquals(generatedClientOrderId.get(), params.get("origClientOrderId"));
            return "{\"orderId\":123456,\"symbol\":\"BTCUSDT\"}";
        });

        BinanceOrderApiDto order = new BinanceOrderApiDto();
        order.setSymbol("BTCUSDT");

        assertEquals(123456L, spotUtil.order(order));
        assertNotNull(generatedClientOrderId.get());
        assertTrue(generatedClientOrderId.get().startsWith("el_"));
        assertTrue(generatedClientOrderId.get().length() <= 36);
    }
}
