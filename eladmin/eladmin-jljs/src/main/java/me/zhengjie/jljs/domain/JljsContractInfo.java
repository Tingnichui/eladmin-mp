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
import java.math.BigDecimal;
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
@TableName("jljs_contract_info")
public class JljsContractInfo implements Serializable {

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

    @NotBlank
    @ApiModelProperty(value = "会员id")
    private String memberId;

    @NotBlank
    @ApiModelProperty(value = "开单教练id")
    private String belongCoachId;

    @ApiModelProperty(value = "合同金额")
    private BigDecimal contractAmount;

    @ApiModelProperty(value = "合同状态;1 未生效；2生效中；3完成；4暂停；5终止；")
    private String contractStatus;

    @ApiModelProperty(value = "使用开始日期")
    private Timestamp useBeginDate;

    @ApiModelProperty(value = "使用结束日期")
    private Timestamp useEndDate;

    @ApiModelProperty(value = "购买日期")
    private Timestamp buyTime;

    @ApiModelProperty(value = "合同备注")
    private String contractRemark;

    @ApiModelProperty(value = "实际收取金额")
    private BigDecimal actualChargeAmount;

    @NotBlank
    @ApiModelProperty(value = "课程id")
    private String courseInfoId;

    @ApiModelProperty(value = "课程类型；1按次消费；2按天计时；")
    private String courseType;

    @ApiModelProperty(value = "课程使用期限")
    private Integer courseUsePeriodDays;

    @ApiModelProperty(value = "课程可使用数量")
    private Integer courseAvailableQuantity;

    @ApiModelProperty(value = "课程剩余数量")
    private Integer courseRemainQuantity;

    @ApiModelProperty(value = "课程已使用数量")
    private Integer courseUseQuantity;

    @ApiModelProperty(value = "课程总暂停天数")
    private Integer courseTotalStopDays;

    public void copy(JljsContractInfo source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
