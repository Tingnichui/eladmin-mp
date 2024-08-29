package me.zhengjie.modules.system.rest;

import cn.hutool.core.net.NetUtil;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.annotation.rest.AnonymousGetMapping;
import me.zhengjie.modules.system.service.HealthService;
import me.zhengjie.utils.RequestHolder;
import me.zhengjie.utils.StringUtils;
import me.zhengjie.utils.enums.HealthStatusEnum;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashSet;


@Slf4j
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Resource
    private HealthService healthService;

    @ApiOperation(value = "健康检查")
    @AnonymousGetMapping(value = "/healthCheck")
    public ResponseEntity healthCheck() {
        ResponseEntity result;
        HealthStatusEnum health = healthService.getHealth();
        if (HealthStatusEnum.DOWN.equals(health)) {
            result = ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(HealthStatusEnum.DOWN.getStatus());
        } else {
            result = ResponseEntity.ok().body(HealthStatusEnum.UP.getStatus());
        }
        return result;
    }

    @ApiOperation(value = "健康状态修改")
    @AnonymousGetMapping(value = "/modifyHealth")
    public ResponseEntity modifyHealth(@RequestParam(value = "sign") String sign, @RequestParam(value = "healthType") String healthType) {
        // 校验本机请求ip
        HttpServletRequest request = RequestHolder.getHttpServletRequest();
        LinkedHashSet<String> localIpSet = NetUtil.localIpv4s();
        String ip = StringUtils.getIp(request);
        log.info("modifyHealth request ip：{}，localIp：{}", ip, StringUtils.join(localIpSet, ","));
        if (!localIpSet.contains(ip)) {
            log.error("非本机ip调用 request ip：{}", ip);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("非法请求，IP不正确");
        }
        if (!healthService.getHealthSign().equals(sign)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("非法请求，密钥不正确");
        }
        if (null == HealthStatusEnum.getByStatus(healthType)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("参数错误");
        }
        healthService.modifyHealth(healthType);
        HealthStatusEnum nowHealthStatus = healthService.getHealth();
        if (healthType.equals(nowHealthStatus.getStatus())) {
            return ResponseEntity.ok().body("修改成功，当前状态：" + nowHealthStatus.getStatus());
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("修改失败，当前状态：" + nowHealthStatus.getStatus());
        }
    }

}
