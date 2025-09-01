package me.zhengjie;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
        BinanceEnum.SYMBOL symbol = BinanceEnum.SYMBOL.BTCUSDT;
        BinanceEnum.KLINES_INTERVAL interval = BinanceEnum.KLINES_INTERVAL.MINUTE_15;
        final long defaultStartTime = DateUtil.parse("2017-08-16", DatePattern.NORM_DATE_PATTERN).getTime();
        investKlinesRecordService.syncKlinesRecord(symbol, interval, defaultStartTime);
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
