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

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import cn.hutool.core.bean.BeanUtil;
import io.swagger.annotations.ApiModelProperty;
import cn.hutool.core.bean.copier.CopyOptions;
import java.sql.Timestamp;
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
@TableName("jljs_contract_info")
public class JljsContractInfo extends BaseEntity implements Serializable {

    @TableId(value = "id")
    @NotNull(groups = BaseEntity.Update.class)
    @ApiModelProperty(value = "主键")
    private String id;

    @TableLogic(value = "0", delval = "1")
    @ApiModelProperty(value = "是否删除;0未删除；秒级时间戳 已删除")
    private String delFlag;

    @NotBlank
    @ApiModelProperty(value = "会员")
    private String memberId;

    @TableField(exist = false)
    @ApiModelProperty(value = "会员姓名")
    private String memberName;

    @NotBlank
    @ApiModelProperty(value = "开单教练")
    private String belongCoachId;

    @TableField(exist = false)
    @ApiModelProperty(value = "开单教练姓名")
    private String belongCoachName;

    @NotNull
    @ApiModelProperty(value = "合同金额")
    private BigDecimal contractAmount;

    @ApiModelProperty(value = "合同状态")
    private String contractStatus;

    @ApiModelProperty(value = "开始日期")
    private Timestamp useBeginDate;

    @ApiModelProperty(value = "结束日期")
    private Timestamp useEndDate;

    @ApiModelProperty(value = "购买日期")
    private Timestamp buyTime;

    @ApiModelProperty(value = "备注")
    private String contractRemark;

    @NotNull
    @ApiModelProperty(value = "实际收取金额")
    private BigDecimal actualChargeAmount;

    @NotBlank
    @ApiModelProperty(value = "课程id")
    private String courseInfoId;

    @TableField(exist = false)
    @ApiModelProperty(value = "课程名称")
    private String courseName;

    @NotBlank
    @ApiModelProperty(value = "课程类型")
    private String courseType;

    @NotNull
    @ApiModelProperty(value = "使用期限")
    private Integer courseUsePeriodDays;

    @NotNull
    @ApiModelProperty(value = "可使用数量")
    private Integer courseAvailableQuantity;

    @ApiModelProperty(value = "剩余数量")
    private Integer courseRemainQuantity;

    @ApiModelProperty(value = "已使用数量")
    private Integer courseUseQuantity;

    @ApiModelProperty(value = "总暂停天数")
    private Integer courseTotalStopDays;

    public void copy(JljsContractInfo source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
