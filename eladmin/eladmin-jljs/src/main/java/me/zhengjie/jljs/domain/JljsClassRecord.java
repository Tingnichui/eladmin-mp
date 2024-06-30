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
package me.zhengjie.jljs.domain;

import lombok.Data;
import cn.hutool.core.bean.BeanUtil;
import io.swagger.annotations.ApiModelProperty;
import cn.hutool.core.bean.copier.CopyOptions;
import java.sql.Timestamp;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import javax.validation.constraints.NotBlank;

/**
* @description /
* @author genghui
* @date 2024-06-30
**/
@Data
@TableName("jljs_class_record")
public class JljsClassRecord implements Serializable {

    @TableId(value = "id")
    @ApiModelProperty(value = "主键")
    private String id;

    @ApiModelProperty(value = "创建人id")
    private String createBy;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新人id")
    private String updateBy;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "是否删除;0未删除；秒级时间戳 已删除")
    private String delFlag;

    @ApiModelProperty(value = "教练id")
    private String coachId;

    @NotBlank
    @ApiModelProperty(value = "会员id")
    private String memberId;

    @ApiModelProperty(value = "关联合同id")
    private String contractInfoId;

    @ApiModelProperty(value = "课程开始时间")
    private Timestamp classBeginTime;

    @ApiModelProperty(value = "课程结束时间")
    private Timestamp classEndTime;

    @ApiModelProperty(value = "课程备注")
    private String classRemark;

    public void copy(JljsClassRecord source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
