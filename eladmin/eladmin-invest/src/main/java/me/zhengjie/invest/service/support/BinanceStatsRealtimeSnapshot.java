package me.zhengjie.invest.service.support;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class BinanceStatsRealtimeSnapshot {

    private BigDecimal currentSpotPrice;
    private BigDecimal usdFuturesPrice;
    private BigDecimal coinFuturesPrice;
    private BigDecimal coinFundingFee;
    private final Map<String, RealtimeStatus> statuses = new LinkedHashMap<>();
    private final List<String> warnings = new ArrayList<>();

    @Data
    public static class RealtimeStatus {
        private String status;
        private Date updatedAt;
        private String message;
        private Long elapsedMillis;

        public RealtimeStatus(String status, Date updatedAt, String message, Long elapsedMillis) {
            this.status = status;
            this.updatedAt = updatedAt;
            this.message = message;
            this.elapsedMillis = elapsedMillis;
        }
    }
}
