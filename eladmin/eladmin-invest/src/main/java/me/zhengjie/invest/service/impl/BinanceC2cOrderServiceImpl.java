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

import me.zhengjie.invest.domain.BinanceC2cOrder;
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
import java.util.LinkedHashMap;
import me.zhengjie.utils.PageResult;

/**
* @description 服务实现
* @author genghui
* @date 2026-09-21
**/
@Service
@RequiredArgsConstructor
public class BinanceC2cOrderServiceImpl extends ServiceImpl<BinanceC2cOrderMapper, BinanceC2cOrder> implements BinanceC2cOrderService {

    private final BinanceC2cOrderMapper binanceC2cOrderMapper;

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
