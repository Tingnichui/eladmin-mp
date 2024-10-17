/*
*  Copyright 2019-2023 Zheng Jie
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
package me.zhengjie.gym.domain;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import cn.hutool.core.bean.BeanUtil;
import io.swagger.annotations.ApiModelProperty;
import cn.hutool.core.bean.copier.CopyOptions;
import java.math.BigDecimal;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import me.zhengjie.base.BaseEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
* @description /
* @author genghui
* @date 2024-06-30
**/
@Data
@TableName("jljs_course_info")
public class GymCourseInfo extends BaseEntity implements Serializable {

    @TableId(value = "id")
    @NotNull(groups = Update.class)
    @ApiModelProperty(value = "主键")
    private String id;

    @TableLogic(value = "0", delval = "1")
    @ApiModelProperty(value = "是否删除;0未删除；秒级时间戳 已删除")
    private String delFlag;

    @NotBlank(message = "名称不能为空")
    @ApiModelProperty(value = "名称")
    private String courseName;

    @NotNull(message = "价格不能为空")
    @ApiModelProperty(value = "价格")
    private BigDecimal coursePrice;

    @ApiModelProperty(value = "描述")
    private String courseDescribe;

    @NotBlank(message = "类型不能为空")
    @ApiModelProperty(value = "类型")
    private String courseType;

    @NotNull(message = "使用期限不能为空")
    @ApiModelProperty(value = "使用期限")
    private Integer courseUsePeriodDays;

    @NotNull(message = "可使用数量不能为空")
    @ApiModelProperty(value = "可使用数量")
    private Integer courseAvailableQuantity;

    public void copy(GymCourseInfo source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
