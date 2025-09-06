package me.zhengjie;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.InvestKlinesRecord;
import me.zhengjie.invest.domain.dto.BinanceFundingRate;
import me.zhengjie.invest.domain.dto.BinanceOrderApiDto;
import me.zhengjie.invest.service.InvestKlinesRecordService;
import me.zhengjie.invest.util.BinanceFuturesUtil;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.utils.DingdingUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UtilsTests {

    @Resource
    private DingdingUtil dingdingUtil;
    @Resource
    private BinanceSpotUtil binanceSpotUtil;
    @Resource
    private BinanceFuturesUtil binanceFuturesUtil;
    @Resource
    private InvestKlinesRecordService investKlinesRecordService;

    @Test
    void sendMsg() {
        dingdingUtil.sendMsg("test");
    }

    @Test
    void getPrice() {
        BigDecimal price = binanceSpotUtil.getPrice(BinanceEnum.SYMBOL.BTCUSDT);
        System.err.println(price);
    }

    @Test
    void getAvgPrice() {
        BigDecimal avgPrice = binanceSpotUtil.getAvgPrice(BinanceEnum.SYMBOL.BTCUSDT);
        System.err.println(avgPrice);
    }

    @Test
    void getKlines() {
        BinanceEnum.SYMBOL symbol = BinanceEnum.SYMBOL.BTCUSDT;
        BinanceEnum.KLINES_INTERVAL interval = BinanceEnum.KLINES_INTERVAL.MINUTE_15;
        final long startTime = 1504713600000L;
        List<InvestKlinesRecord> klines = binanceSpotUtil.getKlines(symbol, interval, startTime, null);
        for (InvestKlinesRecord kline : klines) {
            System.err.println(DateUtil.format(new Date(kline.getOpenTime()), DatePattern.NORM_DATETIME_PATTERN));
        }
    }

    @Test
    void fundingRate() {
        BinanceEnum.SYMBOL symbol = BinanceEnum.SYMBOL.BTCUSDT;
        List<BinanceFundingRate> list = new ArrayList<>();

        long startTime = DateUtil.parse("2024-01-01 00:00:00", DatePattern.NORM_DATETIME_PATTERN).getTime();
        long endTime = DateUtil.parse("2025-01-01 00:00:00", DatePattern.NORM_DATETIME_PATTERN).getTime() - 1;
        while (true) {
            List<BinanceFundingRate> fundingRateList = binanceFuturesUtil.getFundingRate(symbol, startTime, endTime);
            if (CollectionUtils.isEmpty(fundingRateList)) {
                break;
            }
            list.addAll(fundingRateList);
            startTime = fundingRateList.get(fundingRateList.size() - 1).getFundingTime().getTime() + 1;
        }

        BigDecimal totalFundingRate = list.stream()
                .map(item -> new BigDecimal(item.getFundingRate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.err.println(totalFundingRate);

    }

    @Test
    void order() {
        BinanceOrderApiDto apiDto = new BinanceOrderApiDto();
        apiDto.setSymbol("BNBUSDT");
        apiDto.setSide(BinanceEnum.SIDE.BUY);
        apiDto.setType(BinanceEnum.TYPE.LIMIT);
        apiDto.setTimeInForce(BinanceEnum.TIME_IN_FORCE.GTC);
        apiDto.setQuantity(new BigDecimal("0.1"));
        apiDto.setPrice(new BigDecimal("501.1"));
        JSONObject resultJson = binanceSpotUtil.order(apiDto);
        System.err.println(resultJson);
    }
}
