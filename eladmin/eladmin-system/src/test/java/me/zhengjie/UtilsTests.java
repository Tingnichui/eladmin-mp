package me.zhengjie;

import com.alibaba.fastjson.JSONObject;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.dto.BinanceOrderApiDto;
import me.zhengjie.invest.util.BinanceUtil;
import me.zhengjie.utils.DingdingUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UtilsTests {

    @Resource
    private DingdingUtil dingdingUtil;

    @Resource
    private BinanceUtil binanceUtil;

    @Test
    void sendMsg() {
        dingdingUtil.sendMsg("test");
    }

    @Test
    void getPrice() {
        BigDecimal avgPrice = binanceUtil.getPrice("BTCUSDT");
        System.err.println(avgPrice);
    }

    @Test
    void getAvgPrice() {
        BigDecimal avgPrice = binanceUtil.getAvgPrice("BTCUSDT");
        System.err.println(avgPrice);
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
