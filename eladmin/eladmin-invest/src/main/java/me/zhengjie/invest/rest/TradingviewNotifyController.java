package me.zhengjie.invest.rest;


import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.annotation.AnonymousAccess;
import me.zhengjie.annotation.Log;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.dto.BinanceOrderApiDto;
import me.zhengjie.invest.domain.vo.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.domain.vo.BinanceTradeStatsInfoVO;
import me.zhengjie.invest.domain.vo.TradingViewNotify;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceUtil;
import me.zhengjie.utils.DingdingUtil;
import me.zhengjie.utils.RedisUtils;
import me.zhengjie.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequiredArgsConstructor
@Api(tags = "tradingview通知")
@RequestMapping("/api/tradingview/notify")
public class TradingviewNotifyController {

    @Value("${tradingview.notify.secret}")
    public String secret;

    @Resource
    private DingdingUtil dingdingUtil;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private BinanceUtil binanceUtil;
    @Resource
    private BinanceTradeInfoService binanceTradeInfoService;
    @Resource
    private BinanceAccountInfoService binanceAccountInfoService;

    @PostMapping("/trade")
    @Log("tradingview交易通知")
    @AnonymousAccess
    public ResponseEntity<Object> createBinanceTradeInfo(@RequestBody TradingViewNotify tradingViewNotify) {
        if (!secret.equals(tradingViewNotify.getSecret())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        // 发送消息通知
        dingdingUtil.sendMsg("【TradingView-交易通知】" + tradingViewNotify);
        // 判断是否进行交易
        this.doTrade(tradingViewNotify);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void doTrade(TradingViewNotify tradingViewNotify) {
        // 调用接口进行交易
        try {
            final String posId = tradingViewNotify.getPosId();
            final String operateType = tradingViewNotify.getOperateType();
            final String redisKey = posId + "_" + operateType;
            final boolean hasOperate = redisUtils.hasKey(redisKey);


            // 仓位编号不为空，并且还未进行此类操作
            if (StringUtils.isNotBlank(posId) && !hasOperate) {
                final String symbol = tradingViewNotify.getSymbol();
                BinanceOrderApiDto apiDto = new BinanceOrderApiDto();
                apiDto.setSymbol(symbol);
                apiDto.setType(BinanceEnum.TYPE.LIMIT);
                apiDto.setTimeInForce(BinanceEnum.TIME_IN_FORCE.GTC);
                apiDto.setQuantity(new BigDecimal("0.001"));
                apiDto.setPrice(tradingViewNotify.getPrice());
                switch (operateType) {
                    case "OPEN":
                        // 开仓,判断一下仓位，不要在高位买太多
                        BinanceTradeInfoQueryCriteria criteria = new BinanceTradeInfoQueryCriteria();
                        criteria.setSymbol(symbol);
                        criteria.setTradePairingLogic("FIFO");
                        BinanceTradeStatsInfoVO stats = binanceTradeInfoService.stats(criteria);
                        // 当前开仓价格大于剩余未平仓均价 并且 当前未平仓价格已经大于1000u，不调用接口进行操作
                        if (apiDto.getPrice().compareTo(stats.getTotalWaitAvgSellPrice()) > 0 && stats.getTotalWaitSellAmount().compareTo(new BigDecimal("1000")) > 0) {
                            dingdingUtil.sendMsg("剩余未平仓已大于1000u");
                            return;
                        }
                        apiDto.setSide(BinanceEnum.SIDE.BUY);
                        break;
                    case "TAKE_PROFIT":
                        // 止盈
                        apiDto.setSide(BinanceEnum.SIDE.SELL);
                        break;
                    case "STOP_LOSS":
                        // 止损暂时不做
                        return;
                }

                // 查询所有自动交易的账号
                List<BinanceAccountInfo> accountInfoList = binanceAccountInfoService.listAutoTradeAccount();

                for (BinanceAccountInfo accountInfo : accountInfoList) {
                    BinanceAccountContextHolder.runWith(accountInfo, () -> {
                        // 调用接口最多2次
                        int maxRetries  = 2;
                        for (int i = 0; i < maxRetries ; i++) {
                            try {
                                binanceUtil.order(apiDto);
                                dingdingUtil.sendMsg(accountInfo.getIdCardName() + "-调用接口成功;");
                                break;
                            } catch (Exception e) {
                                log.error("下单失败，第 {} 次尝试", i + 1, e);
                            }
                        }
                    });
                }

                // 调用接口成功之后标识
                redisUtils.set(redisKey, "1", 30, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.error("调用币安接口出现异常", e);
            dingdingUtil.sendMsg("调用币安接口出现异常" + e.getMessage());
        }
    }


}
