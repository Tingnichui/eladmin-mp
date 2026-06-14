package me.zhengjie;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceFuturesTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceFundingRate;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceFuturesTradeInfoService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceUsdFuturesUtil;
import me.zhengjie.utils.RedisUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BinanceUsdFuturesUtilTests {

    @Resource
    private BinanceUsdFuturesUtil binanceUsdFuturesUtil;

    @Resource
    private BinanceAccountInfoService binanceAccountInfoService;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private BinanceFuturesTradeInfoService binanceFuturesTradeInfoService;

    @Test
    void userTrades() {
        Set<Long> orderIdSet = binanceFuturesTradeInfoService.list(Wrappers.lambdaQuery(BinanceFuturesTradeInfo.class).select(BinanceFuturesTradeInfo::getOrderId)).stream().map(BinanceFuturesTradeInfo::getOrderId).collect(Collectors.toSet());
        Date now = new Date();
        BinanceAccountInfo accountInfo = binanceAccountInfoService.getAccountByIdCardName("耿辉");
        BinanceAccountContextHolder.runWith(accountInfo, () -> {
            BinanceEnum.SYMBOL symbol = BinanceEnum.SYMBOL.BTCUSDT;
            Date startTime = DateUtil.parse("2025-10-13", DatePattern.NORM_DATE_PATTERN);
            while (true) {
                if (startTime.getTime() > now.getTime()) {
                    break;
                }
                Date endTime = DateUtil.offsetDay(startTime, 7);
                if (endTime.getTime() > now.getTime()) {
                    endTime = now;
                }
                List<BinanceFuturesTradeInfo> binanceFuturesTradeInfos = binanceUsdFuturesUtil.userTrades(symbol, startTime.getTime(), endTime.getTime());
                binanceFuturesTradeInfos.removeIf(v -> orderIdSet.contains(v.getOrderId()));
                if (!binanceFuturesTradeInfos.isEmpty()) {
                    binanceFuturesTradeInfos.forEach(v -> v.setUid(accountInfo.getUid()));
                    binanceFuturesTradeInfoService.saveBatch(binanceFuturesTradeInfos);
                    System.err.println(binanceFuturesTradeInfos);
                }
                startTime = endTime;
            }

        });

    }
    @Test
    void account() {
        BinanceAccountContextHolder.runWith(binanceAccountInfoService.getAccountByIdCardName("耿辉"), () -> {
            JSONObject account = binanceUsdFuturesUtil.account();
            System.err.println(account);
        });
    }

    @Test
    void getLastPosCloseTime() {
        Date lastPosCloseTime = binanceFuturesTradeInfoService.getLastPosCloseTime();
        System.err.println(lastPosCloseTime);
    }

    @Test
    void price() {
        BigDecimal price = binanceUsdFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSDT);
        System.err.println(price);
    }

    @Test
    void fundingRate() {
        BinanceEnum.SYMBOL symbol = BinanceEnum.SYMBOL.BTCUSDT;
        List<BinanceFundingRate> list = new ArrayList<>();

        long startTime = DateUtil.parse("2020-01-01 00:00:00", DatePattern.NORM_DATETIME_PATTERN).getTime();
        long endTime = DateUtil.parse("2025-01-01 00:00:00", DatePattern.NORM_DATETIME_PATTERN).getTime() - 1;
        while (true) {
            List<BinanceFundingRate> fundingRateList = binanceUsdFuturesUtil.getFundingRate(symbol, startTime, endTime);
            if (CollectionUtils.isEmpty(fundingRateList)) {
                break;
            }
            list.addAll(fundingRateList);
            startTime = fundingRateList.get(fundingRateList.size() - 1).getFundingTime().getTime() + 1;
        }

        Map<Integer, BigDecimal> totalFundingRateByYear = list.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getFundingTime().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .getYear(), // 按年份分组
                        Collectors.mapping(
                                item -> new BigDecimal(item.getFundingRate()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add) // 累加
                        )
                ));

        totalFundingRateByYear.forEach((year, total) ->
                System.err.println(year + " 年总资金费率：" + total)
        );

    }
}
