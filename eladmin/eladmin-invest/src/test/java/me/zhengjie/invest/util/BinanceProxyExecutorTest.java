package me.zhengjie.invest.util;

import cn.hutool.http.HttpResponse;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class BinanceProxyExecutorTest {

    @Test
    void shouldFailOverReadRequestToBackupProxy() {
        BinanceProxyExecutor executor = new BinanceProxyExecutor(properties());
        HttpResponse expected = mock(HttpResponse.class);
        List<String> attempts = new ArrayList<>();

        HttpResponse actual = executor.execute(true, proxy -> {
            String host = host(proxy);
            attempts.add(host);
            if ("100.67.168.78".equals(host)) {
                throw new RuntimeException("primary unavailable");
            }
            return expected;
        });

        assertSame(expected, actual);
        assertEquals(Arrays.asList("100.67.168.78", "100.90.6.57"), attempts);
    }

    @Test
    void shouldNotRetryWriteButUseBackupForNextRequest() {
        BinanceProxyExecutor executor = new BinanceProxyExecutor(properties());
        List<String> attempts = new ArrayList<>();

        assertThrows(RuntimeException.class, () -> executor.execute(false, proxy -> {
            attempts.add(host(proxy));
            throw new RuntimeException("write result unknown");
        }));
        assertEquals(Arrays.asList("100.67.168.78"), attempts);

        HttpResponse expected = mock(HttpResponse.class);
        HttpResponse actual = executor.execute(false, proxy -> {
            attempts.add(host(proxy));
            return expected;
        });

        assertSame(expected, actual);
        assertEquals(Arrays.asList("100.67.168.78", "100.90.6.57"), attempts);
    }

    @Test
    void shouldRejectMissingProxyNodes() {
        BinanceProxyProperties properties = new BinanceProxyProperties();
        BinanceProxyExecutor executor = new BinanceProxyExecutor(properties);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> executor.execute(true, proxy -> mock(HttpResponse.class)));

        assertEquals("未配置 binance.proxy.nodes", error.getMessage());
    }

    private BinanceProxyProperties properties() {
        BinanceProxyProperties properties = new BinanceProxyProperties();
        properties.setFailureCooldownMs(60000L);
        properties.setNodes(Arrays.asList(
                node("g3", "100.67.168.78", 7890),
                node("m6", "100.90.6.57", 7890)
        ));
        return properties;
    }

    private BinanceProxyProperties.Node node(String name, String host, int port) {
        BinanceProxyProperties.Node node = new BinanceProxyProperties.Node();
        node.setName(name);
        node.setHost(host);
        node.setPort(port);
        return node;
    }

    private String host(Proxy proxy) {
        return ((InetSocketAddress) proxy.address()).getHostString();
    }
}
