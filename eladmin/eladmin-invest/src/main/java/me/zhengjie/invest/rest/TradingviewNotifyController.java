package me.zhengjie.invest.rest;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import me.zhengjie.annotation.AnonymousAccess;
import me.zhengjie.annotation.Log;
import me.zhengjie.invest.domain.vo.TradingViewNotify;
import me.zhengjie.utils.DingdingUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequiredArgsConstructor
@Api(tags = "tradingview通知")
@RequestMapping("/api/tradingview/notify")
public class TradingviewNotifyController {

    @Value("${tradingview.notify.secret}")
    public String secret ;

    @Resource
    private DingdingUtil dingdingUtil;

    @PostMapping("/trade")
    @Log("tradingview交易通知")
    @AnonymousAccess
    public ResponseEntity<Object> createBinanceTradeInfo(@RequestBody TradingViewNotify tradingViewNotify){
        if (!secret.equals(tradingViewNotify.getSecret())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        dingdingUtil.sendMsg("【TradingView-交易通知】" + tradingViewNotify);
        return new ResponseEntity<>(HttpStatus.OK);
    }


}
