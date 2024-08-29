package me.zhengjie.utils.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum HealthStatusEnum {

    /**
     * 上线
     */
    UP("UP"),

    /**
     * 下线
     */
    DOWN("DOWN");

    private final String status;

    public static HealthStatusEnum getByStatus(String status) {
        for (HealthStatusEnum healthStatusEnum : HealthStatusEnum.values()) {
            if (healthStatusEnum.getStatus().equals(status)) {
                return healthStatusEnum;
            }
        }
        return null;
    }

}
