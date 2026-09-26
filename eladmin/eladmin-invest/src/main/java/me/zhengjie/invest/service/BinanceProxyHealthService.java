package me.zhengjie.invest.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import me.zhengjie.invest.domain.dto.BinanceProxyHealthVO;
import me.zhengjie.invest.util.BinanceProxyExecutor;
import me.zhengjie.invest.util.BinanceProxyProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class BinanceProxyHealthService {

    private static final List<ProbeTarget> TARGETS = Arrays.asList(
            new ProbeTarget("现货", "https://api.binance.com/api/v3/ping"),
            new ProbeTarget("U 本位", "https://fapi.binance.com/fapi/v1/ping"),
            new ProbeTarget("币本位", "https://dapi.binance.com/dapi/v1/ping")
    );

    private final BinanceProxyProperties properties;
    private final BinanceProxyExecutor proxyExecutor;
    private final ExecutorService probeExecutor;

    public BinanceProxyHealthService(BinanceProxyProperties properties,
                                     BinanceProxyExecutor proxyExecutor) {
        this.properties = properties;
        this.proxyExecutor = proxyExecutor;
        this.probeExecutor = Executors.newFixedThreadPool(6, daemonThreadFactory());
    }

    public BinanceProxyHealthVO check() {
        long checkedAt = System.currentTimeMillis();
        List<BinanceProxyProperties.Node> configuredNodes = properties.getNodes() == null
                ? new ArrayList<>() : properties.getNodes();
        List<CompletableFuture<BinanceProxyHealthVO.NodeHealth>> futures = configuredNodes.stream()
                .map(node -> checkNode(node, checkedAt))
                .collect(Collectors.toList());

        List<BinanceProxyHealthVO.NodeHealth> nodes = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        BinanceProxyHealthVO result = new BinanceProxyHealthVO();
        result.setCheckedAt(checkedAt);
        result.setNodes(nodes);
        result.setTotal(nodes.size());
        result.setHealthy((int) nodes.stream().filter(node -> "UP".equals(node.getStatus())).count());
        result.setDegraded((int) nodes.stream().filter(node -> "DEGRADED".equals(node.getStatus())).count());
        result.setUnhealthy((int) nodes.stream().filter(node -> "DOWN".equals(node.getStatus())).count());
        result.setCoolingDown((int) nodes.stream().filter(BinanceProxyHealthVO.NodeHealth::isCoolingDown).count());
        return result;
    }

    private CompletableFuture<BinanceProxyHealthVO.NodeHealth> checkNode(
            BinanceProxyProperties.Node node, long checkedAt) {
        List<CompletableFuture<BinanceProxyHealthVO.ProbeResult>> probes = TARGETS.stream()
                .map(target -> CompletableFuture.supplyAsync(
                        () -> probe(node, target.name, target.url), probeExecutor))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(probes.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> buildNodeHealth(node, checkedAt, probes));
    }

    private BinanceProxyHealthVO.NodeHealth buildNodeHealth(
            BinanceProxyProperties.Node node,
            long checkedAt,
            List<CompletableFuture<BinanceProxyHealthVO.ProbeResult>> probeFutures) {
        List<BinanceProxyHealthVO.ProbeResult> probes = probeFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        long successful = probes.stream().filter(BinanceProxyHealthVO.ProbeResult::isHealthy).count();
        Long cooldownUntil = proxyExecutor.getUnavailableUntil(node);

        BinanceProxyHealthVO.NodeHealth result = new BinanceProxyHealthVO.NodeHealth();
        result.setName(node.displayName());
        result.setAddress(node.getHost() + ":" + node.getPort());
        result.setStatus(successful == probes.size() ? "UP" : successful == 0 ? "DOWN" : "DEGRADED");
        result.setCoolingDown(cooldownUntil != null && cooldownUntil > checkedAt);
        result.setCooldownUntil(cooldownUntil);
        result.setProbes(probes);
        return result;
    }

    protected BinanceProxyHealthVO.ProbeResult probe(
            BinanceProxyProperties.Node node, String targetName, String url) {
        long startedAt = System.nanoTime();
        HttpResponse response = null;
        BinanceProxyHealthVO.ProbeResult result = new BinanceProxyHealthVO.ProbeResult();
        result.setName(targetName);
        try {
            Proxy proxy = new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(node.getHost(), node.getPort()));
            response = HttpRequest.get(url)
                    .setProxy(proxy)
                    .setConnectionTimeout(properties.getConnectTimeoutMs())
                    .setReadTimeout(properties.getReadTimeoutMs())
                    .execute();
            result.setStatusCode(response.getStatus());
            result.setHealthy(response.getStatus() >= 200 && response.getStatus() < 300);
            if (!result.isHealthy()) {
                result.setError("HTTP " + response.getStatus());
            }
        } catch (RuntimeException e) {
            result.setHealthy(false);
            result.setError(errorMessage(e));
        } finally {
            result.setLatencyMs((System.nanoTime() - startedAt) / 1_000_000L);
            if (response != null) {
                response.close();
            }
        }
        return result;
    }

    private String errorMessage(RuntimeException error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return current.getClass().getSimpleName();
        }
        String normalized = message.replaceAll("[\\r\\n]+", " ").trim();
        return normalized.length() > 180 ? normalized.substring(0, 180) + "..." : normalized;
    }

    private ThreadFactory daemonThreadFactory() {
        AtomicInteger sequence = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable,
                    "binance-proxy-health-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    @PreDestroy
    public void destroy() {
        probeExecutor.shutdownNow();
    }

    private static class ProbeTarget {
        private final String name;
        private final String url;

        private ProbeTarget(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }
}
