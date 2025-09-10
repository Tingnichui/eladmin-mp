package me.zhengjie;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceTradeInfoExt;
import me.zhengjie.invest.domain.InvestKlinesRecord;
import me.zhengjie.invest.domain.dto.BinanceFundingRate;
import me.zhengjie.invest.domain.dto.BinanceOrderApiDto;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.InvestKlinesRecordService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BinanceSpotUtilTests {

    @Resource
    private BinanceSpotUtil binanceSpotUtil;

    @Resource
    private BinanceAccountInfoService binanceAccountInfoService;


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
    void order() {
        BinanceOrderApiDto apiDto = new BinanceOrderApiDto();
        apiDto.setSymbol("BNBUSDT");
        apiDto.setSide(BinanceEnum.SIDE.BUY);
        apiDto.setType(BinanceEnum.TYPE.LIMIT);
        apiDto.setTimeInForce(BinanceEnum.TIME_IN_FORCE.GTC);
        apiDto.setQuantity(new BigDecimal("0.1"));
        apiDto.setPrice(new BigDecimal("501.1"));
        Long resultJson = binanceSpotUtil.order(apiDto);
        System.err.println(resultJson);
    }

    @Test
    void listUserOrderHistory() {

        BinanceAccountContextHolder.runWith(binanceAccountInfoService.getAccountByIdCardName("耿辉"), () -> {

            Date now = new Date();
            DateTime startTime = DateUtil.parse("2024-10-01", DatePattern.NORM_DATE_PATTERN);

            List<JSONObject> allRecord = new ArrayList<>();
            while (startTime.isBefore(now)) {
                startTime = DateUtil.beginOfMonth(startTime);
                DateTime endTime = DateUtil.endOfMonth(startTime);
                List<JSONObject> jsonObjects = binanceSpotUtil.listUserOrderHistory(startTime.getTime(), endTime.getTime());
                allRecord.addAll(jsonObjects);

                startTime = DateUtil.offsetMonth(startTime, 1);
            }

            List<JSONObject> successRecord = allRecord.stream()
                    .filter(v -> v.getString("orderStatus").equals("COMPLETED")).collect(Collectors.toList());
            BigDecimal reduce = successRecord
                    .stream().map(v -> v.getBigDecimal("amount"))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            System.err.println(reduce.subtract(new BigDecimal("1000")));

        });
    }


}
