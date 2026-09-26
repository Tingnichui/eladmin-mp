package me.zhengjie.invest.service;

import me.zhengjie.invest.domain.dto.BinanceProxyHealthVO;
import me.zhengjie.invest.util.BinanceProxyExecutor;
import me.zhengjie.invest.util.BinanceProxyProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BinanceProxyHealthServiceTest {

    private BinanceProxyHealthService service;

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.destroy();
        }
    }

    @Test
    void shouldAggregateHealthyDegradedAndDownNodes() {
        BinanceProxyProperties properties = new BinanceProxyProperties();
        properties.setNodes(Arrays.asList(
                node("healthy", "127.0.0.1", 7890),
                node("degraded", "127.0.0.2", 7890),
                node("down", "127.0.0.3", 7890)
        ));
        BinanceProxyExecutor proxyExecutor = new BinanceProxyExecutor(properties);
        service = new BinanceProxyHealthService(properties, proxyExecutor) {
            @Override
            protected BinanceProxyHealthVO.ProbeResult probe(
                    BinanceProxyProperties.Node node, String targetName, String url) {
                boolean healthy = "healthy".equals(node.getName())
                        || ("degraded".equals(node.getName()) && "现货".equals(targetName));
                BinanceProxyHealthVO.ProbeResult result = new BinanceProxyHealthVO.ProbeResult();
                result.setName(targetName);
                result.setHealthy(healthy);
                result.setStatusCode(healthy ? 200 : null);
                result.setLatencyMs(healthy ? 20L : 100L);
                result.setError(healthy ? null : "unavailable");
                return result;
            }
        };

        BinanceProxyHealthVO result = service.check();

        assertEquals(3, result.getTotal());
        assertEquals(1, result.getHealthy());
        assertEquals(1, result.getDegraded());
        assertEquals(1, result.getUnhealthy());
        assertEquals("UP", result.getNodes().get(0).getStatus());
        assertEquals("DEGRADED", result.getNodes().get(1).getStatus());
        assertEquals("DOWN", result.getNodes().get(2).getStatus());
        assertEquals(3, result.getNodes().get(0).getProbes().size());
    }

    private BinanceProxyProperties.Node node(String name, String host, int port) {
        BinanceProxyProperties.Node node = new BinanceProxyProperties.Node();
        node.setName(name);
        node.setHost(host);
        node.setPort(port);
        return node;
    }
}
