package me.zhengjie.invest.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BinanceProxyHealthVO {

    private long checkedAt;
    private int total;
    private int healthy;
    private int degraded;
    private int unhealthy;
    private int coolingDown;
    private List<NodeHealth> nodes = new ArrayList<>();

    @Data
    public static class NodeHealth {
        private String name;
        private String address;
        private String status;
        private boolean coolingDown;
        private Long cooldownUntil;
        private List<ProbeResult> probes = new ArrayList<>();
    }

    @Data
    public static class ProbeResult {
        private String name;
        private boolean healthy;
        private Integer statusCode;
        private long latencyMs;
        private String error;
    }
}
