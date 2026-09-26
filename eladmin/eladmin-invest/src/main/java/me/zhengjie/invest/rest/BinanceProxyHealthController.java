package me.zhengjie.invest.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import me.zhengjie.invest.domain.dto.BinanceProxyHealthVO;
import me.zhengjie.invest.service.BinanceProxyHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Api(tags = "币安代理状态")
@RequestMapping("/api/binanceProxyHealth")
public class BinanceProxyHealthController {

    private final BinanceProxyHealthService binanceProxyHealthService;

    @GetMapping
    @ApiOperation("检测币安代理节点状态")
    @PreAuthorize("@el.check('binanceProxyHealth:list')")
    public ResponseEntity<BinanceProxyHealthVO> check() {
        return ResponseEntity.ok(binanceProxyHealthService.check());
    }
}
