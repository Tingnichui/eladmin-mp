package me.zhengjie.invest.util;

import cn.hutool.http.HttpResponse;
import me.zhengjie.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BinanceProxyExecutor {

    private static final Logger log = LoggerFactory.getLogger(BinanceProxyExecutor.class);

    private final BinanceProxyProperties properties;
    private final Map<String, Long> unavailableUntil = new ConcurrentHashMap<>();

    public BinanceProxyExecutor(BinanceProxyProperties properties) {
        this.properties = properties;
    }

    public HttpResponse execute(boolean failoverAllowed, ProxyRequest request) {
        List<BinanceProxyProperties.Node> candidates = candidates();
        RuntimeException lastError = null;
        int attempts = failoverAllowed ? candidates.size() : Math.min(1, candidates.size());
        for (int i = 0; i < attempts; i++) {
            BinanceProxyProperties.Node node = candidates.get(i);
            try {
                HttpResponse response = request.execute(toProxy(node));
                unavailableUntil.remove(node.key());
                if (i > 0) {
                    log.info("币安请求已切换到备用代理: {}", node.displayName());
                }
                return response;
            } catch (RuntimeException e) {
                lastError = e;
                unavailableUntil.put(node.key(), System.currentTimeMillis() + properties.getFailureCooldownMs());
                log.warn("币安代理调用失败，节点进入冷却: {}, error={}",
                        node.displayName(), e.getClass().getSimpleName());
            }
        }
        throw lastError == null ? new IllegalStateException("没有可用的币安代理节点") : lastError;
    }

    public int getConnectTimeoutMs() {
        return properties.getConnectTimeoutMs();
    }

    public int getReadTimeoutMs() {
        return properties.getReadTimeoutMs();
    }

    /**
     * Returns the current business-request cooldown deadline for a proxy node.
     * Health probes deliberately do not mutate this state.
     */
    public Long getUnavailableUntil(BinanceProxyProperties.Node node) {
        if (node == null) {
            return null;
        }
        String key = node.key();
        Long blockedUntil = unavailableUntil.get(key);
        if (blockedUntil != null && blockedUntil <= System.currentTimeMillis()) {
            unavailableUntil.remove(key, blockedUntil);
            return null;
        }
        return blockedUntil;
    }

    private List<BinanceProxyProperties.Node> candidates() {
        List<BinanceProxyProperties.Node> nodes = properties.getNodes();
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalStateException("未配置 binance.proxy.nodes");
        }
        for (BinanceProxyProperties.Node node : nodes) {
            if (node == null || StringUtils.isBlank(node.getHost()) || node.getPort() <= 0) {
                throw new IllegalStateException("binance.proxy.nodes 存在无效节点");
            }
        }
        long now = System.currentTimeMillis();
        List<BinanceProxyProperties.Node> candidates = new ArrayList<>(nodes);
        candidates.sort(Comparator.comparingLong(node -> {
            Long blockedUntil = unavailableUntil.get(node.key());
            return blockedUntil == null || blockedUntil <= now ? 0L : blockedUntil;
        }));
        return candidates;
    }

    private Proxy toProxy(BinanceProxyProperties.Node node) {
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(node.getHost(), node.getPort()));
    }

    @FunctionalInterface
    public interface ProxyRequest {
        HttpResponse execute(Proxy proxy);
    }
}
