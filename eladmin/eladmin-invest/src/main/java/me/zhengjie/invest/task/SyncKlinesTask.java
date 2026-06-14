package me.zhengjie.invest.task;


import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.service.InvestKlinesRecordService;
import me.zhengjie.utils.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service("syncKlinesTask")
public class SyncKlinesTask {

    @Resource
    private InvestKlinesRecordService investKlinesRecordService;

    public void sync(String params) {
        if (StringUtils.isBlank(params)) {
            throw new RuntimeException("参数不能为空");
        }

        List<JSONObject> paramList = JSON.parseArray(params, JSONObject.class);

        for (JSONObject param : paramList) {
            String symbolStr = param.getString("symbol");
            String intervalStr = param.getString("interval");
            String defaultStartTimeStr = param.getString("defaultStartTime");

            BinanceEnum.SYMBOL symbol = BinanceEnum.SYMBOL.valueOf(symbolStr.toUpperCase());
            BinanceEnum.KLINES_INTERVAL interval = BinanceEnum.KLINES_INTERVAL.valueOf(intervalStr.toUpperCase());
            long defaultStartTime = DateUtil.parse(defaultStartTimeStr, DatePattern.NORM_DATE_PATTERN).getTime();

            investKlinesRecordService.syncKlinesRecord(symbol, interval, defaultStartTime);

        }
    }


}
