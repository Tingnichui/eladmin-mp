package me.zhengjie;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceCoinFuturesTradeInfo;
import me.zhengjie.invest.domain.BinanceFuturesTradeInfo;
import me.zhengjie.invest.domain.InvestKlinesRecord;
import me.zhengjie.invest.domain.dto.BinanceFundingRate;
import me.zhengjie.invest.domain.dto.BinanceOrderApiDto;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceCoinFuturesTradeInfoService;
import me.zhengjie.invest.service.InvestKlinesRecordService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceCoinFuturesUtil;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.invest.util.BinanceUsdFuturesUtil;
import me.zhengjie.utils.DingdingUtil;
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
public class BinanceCoinFuturesUtilTests {

    @Resource
    private BinanceCoinFuturesUtil binanceCoinFuturesUtil;
    @Resource
    private BinanceAccountInfoService binanceAccountInfoService;
    @Resource
    private BinanceCoinFuturesTradeInfoService binanceCoinFuturesTradeInfoService;

    @Test
    void fundingRate() {
        BinanceEnum.SYMBOL symbol = BinanceEnum.SYMBOL.BTCUSD_PERP;
        List<BinanceFundingRate> list = new ArrayList<>();

        long startTime = DateUtil.parse("2020-01-01 00:00:00", DatePattern.NORM_DATETIME_PATTERN).getTime();
        long endTime = DateUtil.parse("2025-01-01 00:00:00", DatePattern.NORM_DATETIME_PATTERN).getTime() - 1;
        while (true) {
            List<BinanceFundingRate> fundingRateList = binanceCoinFuturesUtil.getFundingRate(symbol, startTime, endTime);
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

    @Test
    void listIncome() {
        BinanceEnum.SYMBOL symbol = BinanceEnum.SYMBOL.BTCUSD_PERP;
        long startTime = DateUtil.parse("2025-12-01 00:00:00", DatePattern.NORM_DATETIME_PATTERN).getTime();
        String incomeType = "FUNDING_FEE";

        BinanceAccountInfo accountInfo = binanceAccountInfoService.getAccountByIdCardName("耿辉");
        BinanceAccountContextHolder.runWith(accountInfo, () -> {
            List<JSONObject> jsonObjects = binanceCoinFuturesUtil.listIncome(symbol, startTime, incomeType);
            System.err.println(jsonObjects);
        });
    }

    @Test
    void calculatePositionFundingFee() {
        BinanceAccountInfo accountInfo = binanceAccountInfoService.getAccountByIdCardName("耿辉");
        BinanceAccountContextHolder.runWith(accountInfo, () -> {
            BinanceEnum.SYMBOL symbol = BinanceEnum.SYMBOL.BTCUSD_PERP;
            BigDecimal income = binanceCoinFuturesTradeInfoService.calculatePositionFundingFee(symbol);
            System.err.println(income);
        });
    }

    @Test
    void userTrades() {
        Set<Long> orderIdSet = binanceCoinFuturesTradeInfoService.list(
                Wrappers.lambdaQuery(BinanceCoinFuturesTradeInfo.class)
                        .select(BinanceCoinFuturesTradeInfo::getOrderId)
        ).stream().map(BinanceCoinFuturesTradeInfo::getOrderId).collect(Collectors.toSet());
        Date now = new Date();
        BinanceAccountInfo accountInfo = binanceAccountInfoService.getAccountByIdCardName("耿辉");
        BinanceAccountContextHolder.runWith(accountInfo, () -> {
            BinanceEnum.SYMBOL symbol = BinanceEnum.SYMBOL.BTCUSD_PERP;
            Date startTime = DateUtil.parse("2025-09-01", DatePattern.NORM_DATE_PATTERN);
            while (true) {
                if (startTime.getTime() > now.getTime()) {
                    break;
                }
                Date endTime = DateUtil.offsetDay(startTime, 7);
                if (endTime.getTime() > now.getTime()) {
                    endTime = now;
                }
                List<BinanceCoinFuturesTradeInfo> tradeInfos = binanceCoinFuturesUtil.userTrades(symbol, startTime.getTime(), endTime.getTime());
                tradeInfos.removeIf(v -> orderIdSet.contains(v.getOrderId()));
                if (!tradeInfos.isEmpty()) {
                    tradeInfos.forEach(v -> v.setUid(accountInfo.getUid()));
                    binanceCoinFuturesTradeInfoService.saveBatch(tradeInfos);
                    System.err.println(tradeInfos);
                }
                startTime = endTime;
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        });

    }

}
