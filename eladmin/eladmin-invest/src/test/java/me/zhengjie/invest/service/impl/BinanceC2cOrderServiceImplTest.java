package me.zhengjie.invest.service.impl;

import com.alibaba.fastjson2.JSONObject;
import cn.hutool.core.date.DateUtil;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceC2cOrder;
import me.zhengjie.invest.domain.dto.BinanceC2cAccountAssetsVO;
import me.zhengjie.invest.mapper.BinanceC2cOrderMapper;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceSpotUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BinanceC2cOrderServiceImplTest {

    private final BinanceC2cOrderMapper mapper = mock(BinanceC2cOrderMapper.class);
    private final BinanceAccountInfoService accountInfoService = mock(BinanceAccountInfoService.class);
    private final BinanceSpotUtil binanceSpotUtil = mock(BinanceSpotUtil.class);
    private final BinanceC2cOrderServiceImpl service = new BinanceC2cOrderServiceImpl(
            mapper, accountInfoService, binanceSpotUtil);

    @AfterEach
    void clearContext() {
        BinanceAccountContextHolder.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void syncMapsApiFieldsAndUpsertsOrders() {
        BinanceAccountInfo account = account(7, 1);
        when(accountInfoService.getAccountByUid(7)).thenReturn(account);

        JSONObject record = new JSONObject();
        record.put("orderNumber", "ORDER-1");
        record.put("advNo", "ADV-1");
        record.put("tradeType", "BUY");
        record.put("asset", "USDT");
        record.put("fiat", "CNY");
        record.put("fiatSymbol", "¥");
        record.put("amount", new BigDecimal("100.00"));
        record.put("takerAmount", new BigDecimal("100.00"));
        record.put("totalPrice", new BigDecimal("716.79"));
        record.put("unitPrice", new BigDecimal("7.1679"));
        record.put("orderStatus", "COMPLETED");
        record.put("createTime", 1727740800000L);
        record.put("commission", BigDecimal.ZERO);
        record.put("counterPartNickName", "counterpart");
        record.put("advertisementRole", "TAKER");

        when(binanceSpotUtil.listUserOrderHistory(anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(Collections.singletonList(record), Collections.emptyList());
        when(mapper.upsertBatch(anyList())).thenReturn(1);

        int count = service.sync(7);

        assertEquals(1, count);
        ArgumentCaptor<List<BinanceC2cOrder>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).upsertBatch(captor.capture());
        BinanceC2cOrder order = captor.getValue().get(0);
        assertEquals(Integer.valueOf(7), order.getUid());
        assertEquals("ORDER-1", order.getOrderNumber());
        assertEquals("BUY", order.getTradeType());
        assertEquals(new BigDecimal("716.79"), order.getTotalPrice());
        assertEquals(Long.valueOf(1727740800000L), order.getOrderCreateTime());
        assertEquals(1727740800000L, order.getOrderTime().getTime());
        assertNull(BinanceAccountContextHolder.get());
    }

    @Test
    void syncRejectsAccountWithInvalidApi() {
        when(accountInfoService.getAccountByUid(8)).thenReturn(account(8, 0));

        assertThrows(BadRequestException.class, () -> service.sync(8));

        verify(binanceSpotUtil, never()).listUserOrderHistory(
                anyLong(), anyLong(), anyInt(), anyInt());
        verify(mapper, never()).upsertBatch(anyList());
    }

    @Test
    void syncStartsAtBeginningOfLatestOrderMonth() {
        when(accountInfoService.getAccountByUid(7)).thenReturn(account(7, 1));
        long latestOrderCreateTime = System.currentTimeMillis();
        long expectedStartTime = DateUtil.beginOfMonth(new Date(latestOrderCreateTime)).getTime();
        when(mapper.findLatestOrderCreateTime(7)).thenReturn(latestOrderCreateTime);
        when(binanceSpotUtil.listUserOrderHistory(anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        service.sync(7);

        ArgumentCaptor<Long> startTimeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(binanceSpotUtil).listUserOrderHistory(
                startTimeCaptor.capture(), anyLong(), anyInt(), anyInt());
        assertEquals(expectedStartTime, startTimeCaptor.getValue().longValue());
    }

    @Test
    void getAccountAssetsCalculatesAverageRateFromStoredOrders() {
        BinanceC2cAccountAssetsVO storedAssets = new BinanceC2cAccountAssetsVO();
        storedAssets.setOrderCount(2L);
        storedAssets.setRmbAmount(new BigDecimal("149607.48"));
        storedAssets.setUsdAmount(new BigDecimal("20871.85"));
        storedAssets.setSyncTime(new Timestamp(1727740800000L));
        when(mapper.findAccountAssets(7)).thenReturn(storedAssets);

        BinanceC2cAccountAssetsVO assets = service.getAccountAssets(7);

        assertEquals(Integer.valueOf(7), assets.getUid());
        assertEquals(Long.valueOf(2), assets.getOrderCount());
        assertEquals(new BigDecimal("7.1679"), assets.getRmbToUsdRate());
        assertEquals(storedAssets.getSyncTime(), assets.getSyncTime());
    }

    @Test
    void getAccountAssetsReturnsZeroRateWhenNoOrdersExist() {
        when(mapper.findAccountAssets(8)).thenReturn(null);

        BinanceC2cAccountAssetsVO assets = service.getAccountAssets(8);

        assertEquals(Integer.valueOf(8), assets.getUid());
        assertEquals(BigDecimal.ZERO, assets.getRmbAmount());
        assertEquals(BigDecimal.ZERO, assets.getUsdAmount());
        assertEquals(BigDecimal.ZERO, assets.getRmbToUsdRate());
    }

    private BinanceAccountInfo account(int uid, int apiValidFlag) {
        BinanceAccountInfo account = new BinanceAccountInfo();
        account.setUid(uid);
        account.setApiValidFlag(apiValidFlag);
        return account;
    }
}
