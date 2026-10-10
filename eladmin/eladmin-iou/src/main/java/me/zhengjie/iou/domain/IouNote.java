/*
*  Copyright 2019-2025 Zheng Jie
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
package me.zhengjie.iou.domain;

import lombok.Data;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.alibaba.fastjson2.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.TableField;
import cn.hutool.core.bean.BeanUtil;
import io.swagger.annotations.ApiModelProperty;
import cn.hutool.core.bean.copier.CopyOptions;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.sql.Timestamp;
import java.math.BigDecimal;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
* @description /
* @author chunhui
* @date 2026-10-10
**/
@Data
@TableName("iou_note")
public class IouNote implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "借条ID")
    @JsonSerialize(using = ToStringSerializer.class)
    @JSONField(serializeFeatures = com.alibaba.fastjson2.JSONWriter.Feature.WriteLongAsString)
    private Long id;

    @NotBlank
    @ApiModelProperty(value = "借条编号")
    private String noteNo;

    @NotNull
    @ApiModelProperty(value = "发起人ID")
    @JsonSerialize(using = ToStringSerializer.class)
    @JSONField(serializeFeatures = com.alibaba.fastjson2.JSONWriter.Feature.WriteLongAsString)
    private Long creatorUserId;

    @NotBlank
    @ApiModelProperty(value = "借款人")
    private String borrowerName;

    @ApiModelProperty(value = "借款人身份证HMAC-SHA256值，逻辑关联iou_user.id_no_hash")
    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    @ToString.Exclude
    private String borrowerIdNoHash;

    @ApiModelProperty(value = "借款人身份证号密文快照")
    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    @ToString.Exclude
    private String borrowerIdNoEncrypted;

    @NotBlank
    @ApiModelProperty(value = "出借人")
    private String lenderName;

    @ApiModelProperty(value = "出借人身份证HMAC-SHA256值，逻辑关联iou_user.id_no_hash")
    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    @ToString.Exclude
    private String lenderIdNoHash;

    @ApiModelProperty(value = "出借人身份证号密文快照")
    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    @ToString.Exclude
    private String lenderIdNoEncrypted;

    @NotNull
    @ApiModelProperty(value = "借款类型")
    private Integer loanType;

    @NotNull
    @ApiModelProperty(value = "本金")
    private BigDecimal principalAmount;

    @NotNull
    @ApiModelProperty(value = "年利率（0.15表示15%）")
    private BigDecimal annualRate;

    @NotNull
    @ApiModelProperty(value = "借款日期")
    private Timestamp loanDate;

    @NotNull
    @ApiModelProperty(value = "最终还款日期")
    private Timestamp dueDate;

    @NotNull
    @ApiModelProperty(value = "总利息")
    private BigDecimal interestAmount;

    @NotNull
    @ApiModelProperty(value = "应还本金加利息")
    private BigDecimal totalAmount;

    @ApiModelProperty(value = "借款用途")
    private String purpose;

    @ApiModelProperty(value = "备注")
    private String remark;

    @NotNull
    @ApiModelProperty(value = "服务费")
    private BigDecimal serviceFee;

    @ApiModelProperty(value = "借款人签署时间")
    private Timestamp borrowerSignedAt;

    @ApiModelProperty(value = "出借人签署时间")
    private Timestamp lenderSignedAt;

    @ApiModelProperty(value = "协议文件存储标识")
    private String agreementFile;

    @NotNull
    @ApiModelProperty(value = "借条状态")
    private Integer status;

    @ApiModelProperty(value = "生效时间")
    private Timestamp effectiveAt;

    @ApiModelProperty(value = "取消原因")
    private String cancelReason;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;


    @TableField(exist = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JSONField(serialize = false)
    @ToString.Exclude
    private String borrowerIdNo;

    @TableField(exist = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JSONField(serialize = false)
    @ToString.Exclude
    private String lenderIdNo;

    public void copy(IouNote source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
