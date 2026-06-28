package me.zhengjie;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.InvestKlinesRecord;
import me.zhengjie.invest.domain.dto.BinanceFundingRate;
import me.zhengjie.invest.domain.dto.BinanceOrderApiDto;
import me.zhengjie.invest.service.InvestKlinesRecordService;
import me.zhengjie.invest.task.SyncKlinesTask;
import me.zhengjie.invest.util.BinanceUsdFuturesUtil;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.utils.AliyunOcrUtil;
import me.zhengjie.utils.DingdingUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.File;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UtilsTests {

    @Resource
    private DingdingUtil dingdingUtil;
    @Resource
    private BinanceSpotUtil binanceSpotUtil;
    @Resource
    private BinanceUsdFuturesUtil binanceUsdFuturesUtil;
    @Resource
    private InvestKlinesRecordService investKlinesRecordService;
    @Resource
    private AliyunOcrUtil aliyunOcrUtil;
    @Resource
    private SyncKlinesTask syncKlinesTask;

    @Test
    void recognizeGeneral() {
        File file = new File("C:\\Users\\chunhui\\Pictures\\Saved Pictures\\open.png");
        String res = aliyunOcrUtil.recognizeDocumentStructure(file);
        System.err.println(res);
    }

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
        String params = "[\n" +
                "    {\n" +
                "        \"symbol\": \"BTCUSDT\",\n" +
                "        \"intervalCodes\": \"5m,15m,30m,1h,2h,4h,6h,8h,12h,1d,3d,1w,1M\",\n" +
                "        \"defaultStartTime\": \"2017-08-16\"\n" +
                "    }\n" +
                "]";
        syncKlinesTask.sync(params);

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
}
