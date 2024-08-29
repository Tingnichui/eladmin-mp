package me.zhengjie.modules.system.service.impl;

import me.zhengjie.modules.system.service.HealthService;
import me.zhengjie.utils.enums.HealthStatusEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HealthServiceImpl implements HealthService {

    @Value("${eladmin.health.sign:s5RybsyR8MFgA3xR}")
    private String HEALTH_SIGN;

    private static HealthStatusEnum HEALTH = HealthStatusEnum.UP;

    @Override
    public String getHealthSign() {
        return HEALTH_SIGN;
    }

    @Override
    public HealthStatusEnum getHealth() {
        return HEALTH;
    }

    @Override
    public void modifyHealth(String healthType) {
        HEALTH = HealthStatusEnum.getByStatus(healthType);
    }

}
