/*
*  Copyright 2019-2025 Zheng Jie
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/
package me.zhengjie.invest.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import me.zhengjie.invest.domain.BinanceC2cOrder;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.BinanceC2cOrderService;
import me.zhengjie.invest.domain.dto.BinanceC2cOrderQueryCriteria;
import me.zhengjie.invest.mapper.BinanceC2cOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.zhengjie.utils.PageUtil;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.sql.Timestamp;
import me.zhengjie.utils.PageResult;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
* @description 服务实现
* @author genghui
* @date 2026-09-21
**/
@Service
@RequiredArgsConstructor
public class BinanceC2cOrderServiceImpl extends ServiceImpl<BinanceC2cOrderMapper, BinanceC2cOrder> implements BinanceC2cOrderService {

    private static final String SYNC_START_DATE = "2024-10-01";
    private static final int API_PAGE_SIZE = 100;
    private static final int UPSERT_BATCH_SIZE = 200;

    private final BinanceC2cOrderMapper binanceC2cOrderMapper;
    private final BinanceAccountInfoService binanceAccountInfoService;
    private final BinanceSpotUtil binanceSpotUtil;

    @Override
    public int sync(Integer uid) {
        BinanceAccountInfo accountInfo = binanceAccountInfoService.getAccountByUid(uid);
        if (!Integer.valueOf(1).equals(accountInfo.getApiValidFlag())) {
            throw new BadRequestException("当前账户 API 不可用");
        }

        int[] syncedCount = {0};
        BinanceAccountContextHolder.runWith(accountInfo, () -> syncedCount[0] = syncCurrentAccount(uid));
        return syncedCount[0];
    }

    private int syncCurrentAccount(Integer uid) {
        Date now = new Date();
        DateTime cursor = DateUtil.parse(SYNC_START_DATE, DatePattern.NORM_DATE_PATTERN);
        Timestamp syncTime = new Timestamp(now.getTime());
        Set<String> syncedOrderNumbers = new HashSet<>();
        int syncedCount = 0;

        while (!cursor.isAfter(now)) {
            DateTime monthStart = DateUtil.beginOfMonth(cursor);
            DateTime monthEnd = DateUtil.endOfMonth(cursor);
            long rangeEnd = Math.min(monthEnd.getTime(), now.getTime());
            syncedCount += syncRange(uid, monthStart.getTime(), rangeEnd,
                    syncTime, syncedOrderNumbers);
            cursor = DateUtil.offsetMonth(monthStart, 1);
        }
        return syncedCount;
    }

    private int syncRange(Integer uid,
                          long startTimestamp,
                          long endTimestamp,
                          Timestamp syncTime,
                          Set<String> syncedOrderNumbers) {
        int page = 1;
        int syncedCount = 0;
        while (true) {
            List<JSONObject> records = binanceSpotUtil.listUserOrderHistory(
                    startTimestamp, endTimestamp, page, API_PAGE_SIZE);
            if (CollectionUtils.isEmpty(records)) {
                break;
            }

            List<BinanceC2cOrder> orders = new ArrayList<>();
            for (JSONObject record : records) {
                BinanceC2cOrder order = toOrder(uid, record, syncTime);
                if (syncedOrderNumbers.add(order.getOrderNumber())) {
                    orders.add(order);
                }
            }
            if (orders.isEmpty()) {
                throw new IllegalStateException("币安 C2C 订单分页未向前推进");
            }
            upsertInBatches(orders);
            syncedCount += orders.size();

            if (records.size() < API_PAGE_SIZE) {
                break;
            }
            page++;
        }
        return syncedCount;
    }

    private void upsertInBatches(List<BinanceC2cOrder> orders) {
        for (int from = 0; from < orders.size(); from += UPSERT_BATCH_SIZE) {
            int to = Math.min(from + UPSERT_BATCH_SIZE, orders.size());
            binanceC2cOrderMapper.upsertBatch(orders.subList(from, to));
        }
    }

    private BinanceC2cOrder toOrder(Integer uid, JSONObject record, Timestamp syncTime) {
        String orderNumber = record.getString("orderNumber");
        Long orderCreateTime = record.getLong("createTime");
        if (!StringUtils.hasText(orderNumber) || orderCreateTime == null
                || !StringUtils.hasText(record.getString("tradeType"))
                || !StringUtils.hasText(record.getString("asset"))
                || !StringUtils.hasText(record.getString("fiat"))
                || !StringUtils.hasText(record.getString("orderStatus"))
                || record.getBigDecimal("totalPrice") == null) {
            throw new IllegalStateException("币安 C2C 订单缺少必填字段：" + orderNumber);
        }

        BinanceC2cOrder order = new BinanceC2cOrder();
        order.setUid(uid);
        order.setOrderNumber(orderNumber);
        order.setAdvNo(record.getString("advNo"));
        order.setTradeType(record.getString("tradeType"));
        order.setAsset(record.getString("asset"));
        order.setFiat(record.getString("fiat"));
        order.setFiatSymbol(record.getString("fiatSymbol"));
        order.setAmount(record.getBigDecimal("amount"));
        order.setTakerAmount(record.getBigDecimal("takerAmount"));
        order.setTotalPrice(record.getBigDecimal("totalPrice"));
        order.setUnitPrice(record.getBigDecimal("unitPrice"));
        order.setOrderStatus(record.getString("orderStatus"));
        order.setOrderCreateTime(orderCreateTime);
        order.setOrderTime(new Timestamp(orderCreateTime));
        order.setCommission(record.getBigDecimal("commission"));
        order.setCounterPartNickName(record.getString("counterPartNickName"));
        order.setAdvertisementRole(record.getString("advertisementRole"));
        order.setRawData(JSON.toJSONString(record));
        order.setSyncTime(syncTime);
        return order;
    }

    @Override
    public PageResult<BinanceC2cOrder> queryAll(BinanceC2cOrderQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(binanceC2cOrderMapper.findAll(criteria, page));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(BinanceC2cOrder resources) {
        binanceC2cOrderMapper.insert(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BinanceC2cOrder resources) {
        BinanceC2cOrder binanceC2cOrder = getById(resources.getId());
        binanceC2cOrder.copy(resources);
        binanceC2cOrderMapper.updateById(binanceC2cOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        binanceC2cOrderMapper.deleteBatchIds(ids);
    }

    @Override
    public void download(BinanceC2cOrderQueryCriteria criteria, HttpServletResponse response) throws IOException {
        List<String> headers = new ArrayList<>();
        headers.add("币安账户用户编号");
        headers.add("币安订单号（orderNumber）");
        headers.add("广告编号（advNo）");
        headers.add("交易方向（tradeType）：BUY、SELL");
        headers.add("数字资产（asset），如 USDT");
        headers.add("法币（fiat），如 CNY");
        headers.add("法币符号（fiatSymbol）");
        headers.add("数字资产数量（amount）");
        headers.add("数字资产数量（takerAmount）");
        headers.add("法币总金额（totalPrice）");
        headers.add("成交单价（unitPrice）");
        headers.add("订单状态（orderStatus）");
        headers.add("币安订单创建时间戳（createTime，毫秒）");
        headers.add("币安订单创建时间（本地转换值）");
        headers.add("手续费（commission）");
        headers.add("交易对手昵称（counterPartNickName）");
        headers.add("广告角色（advertisementRole）");
        headers.add("币安接口原始 JSON");
        headers.add("最近同步时间");
        headers.add("创建时间");
        headers.add("更新时间");
        FileUtil.downloadExcel(headers, response, writer -> {
            long current = 1L;
            final long pageSize = 10000L;
            while (true) {
                Page<Object> page = new Page<>(current, pageSize, false);
                List<BinanceC2cOrder> records = binanceC2cOrderMapper.findAll(criteria, page).getRecords();
                if (records.isEmpty()) {
                    break;
                }
                List<Map<String, Object>> list = new ArrayList<>(records.size());
                for (BinanceC2cOrder binanceC2cOrder : records) {
                    Map<String,Object> map = new LinkedHashMap<>();
                    map.put("币安账户用户编号", binanceC2cOrder.getUid());
                    map.put("币安订单号（orderNumber）", binanceC2cOrder.getOrderNumber());
                    map.put("广告编号（advNo）", binanceC2cOrder.getAdvNo());
                    map.put("交易方向（tradeType）：BUY、SELL", binanceC2cOrder.getTradeType());
                    map.put("数字资产（asset），如 USDT", binanceC2cOrder.getAsset());
                    map.put("法币（fiat），如 CNY", binanceC2cOrder.getFiat());
                    map.put("法币符号（fiatSymbol）", binanceC2cOrder.getFiatSymbol());
                    map.put("数字资产数量（amount）", binanceC2cOrder.getAmount());
                    map.put("数字资产数量（takerAmount）", binanceC2cOrder.getTakerAmount());
                    map.put("法币总金额（totalPrice）", binanceC2cOrder.getTotalPrice());
                    map.put("成交单价（unitPrice）", binanceC2cOrder.getUnitPrice());
                    map.put("订单状态（orderStatus）", binanceC2cOrder.getOrderStatus());
                    map.put("币安订单创建时间戳（createTime，毫秒）", getExportValue(binanceC2cOrder.getOrderCreateTime()));
                    map.put("币安订单创建时间（本地转换值）", binanceC2cOrder.getOrderTime());
                    map.put("手续费（commission）", binanceC2cOrder.getCommission());
                    map.put("交易对手昵称（counterPartNickName）", binanceC2cOrder.getCounterPartNickName());
                    map.put("广告角色（advertisementRole）", binanceC2cOrder.getAdvertisementRole());
                    map.put("币安接口原始 JSON", binanceC2cOrder.getRawData());
                    map.put("最近同步时间", binanceC2cOrder.getSyncTime());
                    map.put("创建时间", binanceC2cOrder.getCreateTime());
                    map.put("更新时间", binanceC2cOrder.getUpdateTime());
                    list.add(map);
                }
                writer.write(list);
                if (records.size() < pageSize) {
                    break;
                }
                current++;
            }
        });
    }

    private Object getExportValue(Object value) {
        return value instanceof Long ? String.valueOf(value) : value;
    }
}
