/*
 *  Copyright 2019-2020 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package me.zhengjie.utils.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * <p>
 * 验证码业务场景对应的 Redis 中的 key
 * </p>
 * @author Zheng Jie
 * @date 2020-05-02
 */
@Getter
@AllArgsConstructor
public enum OrderDirectionEnum {

    ASC("asc", "正序"),

    DESC("desc", "倒序"),
    ;

    private final String value;
    private final String description;

    public static OrderDirectionEnum getByValue(String value){
        for (OrderDirectionEnum directionEnum : values()) {
            if (directionEnum.getValue().equals(value)) {
                return directionEnum;
            }
        }
        return null;
    }
}
