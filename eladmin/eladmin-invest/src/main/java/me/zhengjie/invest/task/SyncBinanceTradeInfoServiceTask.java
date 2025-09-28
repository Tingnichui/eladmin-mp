package me.zhengjie.invest.task;


import lombok.extern.slf4j.Slf4j;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.utils.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("syncBinanceTradeInfoServiceTask")
public class SyncBinanceTradeInfoServiceTask {

    @Resource
    private BinanceTradeInfoService binanceTradeInfoService;

    public void sync(String symbols) {
        if (org.apache.commons.lang3.StringUtils.isBlank(symbols)) {
            throw new RuntimeException("交易对不能为空");
        }

        List<String> symbolList = Arrays.stream(symbols.split(",")).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        for (String symbol : symbolList) {
            binanceTradeInfoService.syncTradeInfo(symbol);
        }
    }


}
