package me.zhengjie;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.InvestKlinesRecord;
import me.zhengjie.invest.domain.dto.BinanceFundingRate;
import me.zhengjie.invest.domain.dto.BinanceOrderApiDto;
import me.zhengjie.invest.service.InvestKlinesRecordService;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BinanceCoinFuturesUtilTests {

    @Resource
    private BinanceCoinFuturesUtil binanceCoinFuturesUtil;

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

}
