package me.zhengjie.invest.domain.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import me.zhengjie.invest.constants.BinanceEnum;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class BinanceOrderApiDto {

    private String symbol;// YES
    private BinanceEnum.SIDE side;// YES	详见枚举定义：订单方向
    private BinanceEnum.TYPE type;// YES	详见枚举定义：订单类型
    private BinanceEnum.TIME_IN_FORCE timeInForce;// NO	详见枚举定义：生效时间
    private BigDecimal quantity;// NO
    private BigDecimal quoteOrderQty;// NO
    private BigDecimal price;// NO
    private String newClientOrderId;// NO	用户自定义的orderid，如空缺系统会自动赋值。
    private Long strategyId;// NO
    private Integer strategyType;// NO	不能低于 1000000.
    private BigDecimal stopPrice;// NO	仅 STOP_LOSS, STOP_LOSS_LIMIT, TAKE_PROFIT, TAKE_PROFIT_LIMIT 需要此参数。
    private Long trailingDelta;// NO	参见 追踪止盈止损(Trailing Stop)订单常见问题。
    private BigDecimal icebergQty;// NO	仅有限价单(包括条件限价单与限价做事单)可以使用该参数，含义为创建冰山订单并指定冰山订单的数量。
    private BinanceEnum.NEW_ORDER_RESP_TYPE newOrderRespType;// NO	指定响应类型 ACK, RESULT, or FULL; MARKET 与 LIMIT 订单默认为FULL, 其他默认为ACK。
    private BinanceEnum.STP selfTradePreventionMode;// NO	允许的 ENUM 取决于交易对的配置。支持的值有：STP 模式。
    private Long recvWindow;// NO
    private Long timestamp;// YES

    public Map<String, Object> toMap() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(this, Map.class);
    }


}
