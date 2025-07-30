package me.zhengjie;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.InvestKlinesRecord;
import me.zhengjie.invest.domain.dto.BinanceOrderApiDto;
import me.zhengjie.invest.service.InvestKlinesRecordService;
import me.zhengjie.invest.util.BinanceUtil;
import me.zhengjie.utils.DingdingUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UtilsTests {

    @Resource
    private DingdingUtil dingdingUtil;

    @Resource
    private BinanceUtil binanceUtil;

    @Resource
    private InvestKlinesRecordService investKlinesRecordService;

    @Test
    void sendMsg() {
        dingdingUtil.sendMsg("test");
    }

    @Test
    void getPrice() {
        BigDecimal price = binanceUtil.getPrice(BinanceEnum.SYMBOL.BTCUSDT);
        System.err.println(price);
    }

    @Test
    void getAvgPrice() {
        BigDecimal avgPrice = binanceUtil.getAvgPrice(BinanceEnum.SYMBOL.BTCUSDT);
        System.err.println(avgPrice);
    }

    @Test
    void getKlines() {
        Date now = new Date();
        DateTime startTime = DateUtil.parse("2017-8-11", DatePattern.NORM_DATE_PATTERN);
        while (now.after(startTime)) {
            DateTime endTime = DateUtil.offsetDay(startTime, 10).offset(DateField.SECOND, -1);
            List<InvestKlinesRecord> klines = binanceUtil.getKlines(BinanceEnum.SYMBOL.BTCUSDT, BinanceEnum.KLINES_INTERVAL.MINUTE_15, startTime, endTime);
            investKlinesRecordService.saveBatch(klines);
            startTime = endTime;
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
        JSONObject resultJson = binanceUtil.order(apiDto);
        System.err.println(resultJson);
    }
}
