package me.zhengjie.modules.system.service;

import me.zhengjie.utils.enums.HealthStatusEnum;

public interface HealthService {

    /**
     * 获取健康检查sign
     *
     * @return
     */
    String getHealthSign();

    /**
     * 获取服务健康状态
     *
     * @return
     */
    HealthStatusEnum getHealth();


    /**
     * 修改服务健康状态
     *
     * @param healthType
     */
    void modifyHealth(String healthType);

}
