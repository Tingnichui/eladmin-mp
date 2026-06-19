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
            String intervalCodes = param.getString("intervalCodes");
            String defaultStartTimeStr = param.getString("defaultStartTime");

            BinanceEnum.SYMBOL symbol = BinanceEnum.SYMBOL.valueOf(symbolStr.toUpperCase());
            long defaultStartTime = DateUtil.parse(defaultStartTimeStr, DatePattern.NORM_DATE_PATTERN).getTime();

            for (final String intervalCode : intervalCodes.split(",")) {
                investKlinesRecordService.syncKlinesRecord(symbol, intervalCode, defaultStartTime);
            }

        }
    }


}
