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
@TableName("jljs_contract_operate_record")
public class JljsContractOperateRecord implements Serializable {

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

    @ApiModelProperty(value = "合同id")
    private String contractInfoId;

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

    @NotBlank
    @ApiModelProperty(value = "操作状态；1成功2撤销")
    private String operateStatus;

    public void copy(JljsContractOperateRecord source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
