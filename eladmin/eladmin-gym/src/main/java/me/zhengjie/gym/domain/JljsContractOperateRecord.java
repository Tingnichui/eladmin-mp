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
@TableName("jljs_contract_operate_record")
public class JljsContractOperateRecord extends BaseEntity implements Serializable {

    @TableId(value = "id")
    @NotNull(groups = BaseEntity.Update.class)
    @ApiModelProperty(value = "主键")
    private String id;

    @TableLogic(value = "0", delval = "1")
    @ApiModelProperty(value = "是否删除;0未删除；秒级时间戳 已删除")
    private String delFlag;

    @NotBlank(message = "合同id不能为空")
    @ApiModelProperty(value = "合同id")
    private String contractInfoId;

    @NotBlank(message = "合同操作类型不能为空")
    @ApiModelProperty(value = "合同操作类型")
    private String contractOperateType;

    @ApiModelProperty(value = "间隔天数")
    private Integer intervalDays;

    @ApiModelProperty(value = "开始时间")
    private Timestamp operateBeginDate;

    @ApiModelProperty(value = "结束时间")
    private Timestamp operateEndDate;

    @ApiModelProperty(value = "操作原因")
    private String operateReason;

    @ApiModelProperty(value = "操作金额")
    private String operateAmount;

    @ApiModelProperty(value = "状态")
    private String operateStatus;

    @ApiModelProperty(value = "会员姓名")
    @TableField(exist = false)
    private String memberName;

    @ApiModelProperty(value = "课程")
    @TableField(exist = false)
    private String courseName;

    public void copy(JljsContractOperateRecord source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
