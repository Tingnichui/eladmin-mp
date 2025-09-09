package me.zhengjie.invest.constants;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BinanceEnum {

    @Getter
    @AllArgsConstructor
    public enum SYMBOL {

        BTCUSDT("BTC现货"),
        BNBUSDT("BNC现货"),
        BTCUSD_PERP("BTC合约"),
        ;
        private final String desc;
    }

    @Getter
    @AllArgsConstructor
    public enum SIDE {

        BUY("买入"),
        SELL("卖出"),
        ;
        private final String desc;
    }

    @Getter
    @AllArgsConstructor
    public enum TYPE {

        LIMIT("限价单"),
        MARKET("市价单"),
        STOP_LOSS("止损单"),
        STOP_LOSS_LIMIT("限价止损单"),
        TAKE_PROFIT("止盈单"),
        TAKE_PROFIT_LIMIT("限价止盈单"),
        LIMIT_MAKER("限价做市单"),
        ;
        private final String desc;
    }

    @Getter
    @AllArgsConstructor
    public enum TIME_IN_FORCE {

        GTC("成交为止，订单会一直有效，直到被成交或者取消。"),
        IOC("无法立即成交的部分就撤销，订单在失效前会尽量多的成交。"),
        FOK("无法全部立即成交就撤销，如果无法全部成交，订单会失效。"),
        ;
        private final String desc;
    }

    @Getter
    @AllArgsConstructor
    public enum NEW_ORDER_RESP_TYPE {

        ACK(""),
        RESULT(""),
        FULL(""),
        ;
        private final String desc;
    }

    @Getter
    @AllArgsConstructor
    public enum STP {

        NONE("此模式使订单免于自我交易预防。"),
        EXPIRE_TAKER("此模式通过立即使吃单者(taker)的剩余数量过期来预防交易。"),
        EXPIRE_MAKER("此模式通过立即使潜在挂单者(maker)的剩余数量过期来预防交易。"),
        EXPIRE_BOTH("此模式通过立即同时使吃单和挂单者的剩余数量过期来预防交易。"),
        DECREMENT("此模式通过阻止匹配的数量来增加两种订单的 prevented quantity。这将使可用数量较少的订单过期， 如果两个订单的可用数量相等，那么两个订单都将过期。"),
        ;
        private final String desc;
    }


    @Getter
    @AllArgsConstructor
    public enum KLINES_INTERVAL {

        MINUTE_15("15m", 15, "15分钟"),
        ;
        private final String value;
        private final int period;
        private final String desc;
    }



}
