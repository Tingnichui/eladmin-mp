package me.zhengjie.invest.util;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "binance.proxy")
public class BinanceProxyProperties {

    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 10000;
    private long failureCooldownMs = 60000L;
    private List<Node> nodes = new ArrayList<>();

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public long getFailureCooldownMs() {
        return failureCooldownMs;
    }

    public void setFailureCooldownMs(long failureCooldownMs) {
        this.failureCooldownMs = failureCooldownMs;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public static class Node {

        private String name;
        private String host;
        private int port;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String displayName() {
            return name == null || name.trim().isEmpty() ? host + ":" + port : name;
        }

        public String key() {
            return host + ":" + port;
        }
    }
}
