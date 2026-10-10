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
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import cn.hutool.core.bean.BeanUtil;
import io.swagger.annotations.ApiModelProperty;
import cn.hutool.core.bean.copier.CopyOptions;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.sql.Timestamp;
import javax.validation.constraints.NotBlank;
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
@TableName("iou_user")
public class IouUser implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    @JSONField(serializeFeatures = com.alibaba.fastjson2.JSONWriter.Feature.WriteLongAsString)
    private Long id;

    @NotBlank
    @ApiModelProperty(value = "手机号")
    private String mobile;

    @ApiModelProperty(value = "登录密码哈希（BCrypt），禁止存储明文")
    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    @ToString.Exclude
    private String passwordHash;

    @ApiModelProperty(value = "真实姓名")
    private String realName;

    @ApiModelProperty(value = "身份证号密文")
    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    @ToString.Exclude
    private String idNoEncrypted;

    @ApiModelProperty(value = "身份证号HMAC-SHA256值，用于认证通过后的登录查询和唯一绑定")
    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    @ToString.Exclude
    private String idNoHash;

    @ApiModelProperty(value = "认证状态")
    private Integer verifyStatus;

    @ApiModelProperty(value = "认证通过时间")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Timestamp verifiedAt;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;


    @TableField(exist = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JSONField(serialize = false)
    @ToString.Exclude
    private String password;

    @TableField(exist = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JSONField(serialize = false)
    @ToString.Exclude
    private String idNo;

    public void copy(IouUser source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
