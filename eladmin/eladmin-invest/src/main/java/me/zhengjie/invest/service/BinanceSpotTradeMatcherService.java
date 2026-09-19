package me.zhengjie.invest.service;

import me.zhengjie.invest.domain.dto.BinanceSpotTradeMatchResult;

/**
 * 现货成交数据库 FIFO 撮合服务。
 */
public interface BinanceSpotTradeMatcherService {

    int initializeStates(Integer uid, String symbol);

    BinanceSpotTradeMatchResult match(Integer uid, String symbol);

    BinanceSpotTradeMatchResult initializeAndMatch(Integer uid, String symbol);
}
