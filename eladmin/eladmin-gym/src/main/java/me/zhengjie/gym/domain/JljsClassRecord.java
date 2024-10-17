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

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import cn.hutool.core.bean.BeanUtil;
import io.swagger.annotations.ApiModelProperty;
import cn.hutool.core.bean.copier.CopyOptions;
import java.sql.Timestamp;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import me.zhengjie.base.BaseEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
* @description /
* @author genghui
* @date 2024-07-02
**/
@Data
@TableName("jljs_class_record")
public class JljsClassRecord extends BaseEntity implements Serializable {

    @TableId(value = "id")
    @NotNull(groups = BaseEntity.Update.class)
    @ApiModelProperty(value = "主键")
    private String id;

    @TableLogic(value = "0", delval = "1")
    @ApiModelProperty(value = "是否删除;0未删除；秒级时间戳 已删除")
    private String delFlag;

    @NotBlank(message = "教练id不能为空")
    @ApiModelProperty(value = "教练id")
    private String coachId;

    @TableField(exist = false)
    @ApiModelProperty(value = "教练名称")
    private String coachName;

    @NotBlank(message = "会员id不能为空")
    @ApiModelProperty(value = "会员id")
    private String memberId;

    @ApiModelProperty(value = "关联合同id")
    private String contractInfoId;

    @NotNull(message = "课程开始时间不能为空")
    @ApiModelProperty(value = "课程开始时间")
    private Timestamp classBeginTime;

    @NotNull(message = "课程结束时间不能为空")
    @ApiModelProperty(value = "课程结束时间")
    private Timestamp classEndTime;

    @ApiModelProperty(value = "课程备注")
    private String classRemark;

    public void copy(JljsClassRecord source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
