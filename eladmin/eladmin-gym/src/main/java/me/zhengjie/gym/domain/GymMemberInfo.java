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

import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import me.zhengjie.annotation.validation.MobilePhone;
import me.zhengjie.base.BaseEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
* @description /
* @author genghui
* @date 2024-06-30
**/
@Data
@TableName("jljs_member_info")
public class GymMemberInfo extends BaseEntity implements Serializable {

    @TableId(value = "id")
    @NotNull(groups = BaseEntity.Update.class)
    @ApiModelProperty(value = "主键")
    private String id;

    @TableLogic(value = "0", delval = "1")
    @ApiModelProperty(value = "是否删除;0未删除；秒级时间戳 已删除")
    private String delFlag;

    @NotBlank(message = "姓名不能为空")
    @ApiModelProperty(value = "姓名")
    private String memberName;

    @ApiModelProperty(value = "性别")
    private String memberGender;

    @ApiModelProperty(value = "年龄")
    private String memberAge;

    @MobilePhone
    @ApiModelProperty(value = "手机号")
    private String memberPhoneNum;

    @ApiModelProperty(value = "生日")
    private String birthDay;

    @ApiModelProperty(value = "用户id")
    private Long userId;

    public void copy(GymMemberInfo source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
