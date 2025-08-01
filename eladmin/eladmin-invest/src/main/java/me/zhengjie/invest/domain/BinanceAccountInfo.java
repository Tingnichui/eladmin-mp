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
package me.zhengjie.invest.domain;

import lombok.Data;
import cn.hutool.core.bean.BeanUtil;
import io.swagger.annotations.ApiModelProperty;
import cn.hutool.core.bean.copier.CopyOptions;
import java.math.BigDecimal;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
* @description /
* @author genghui
* @date 2025-08-02
**/
@Data
@TableName("binance_account_info")
public class BinanceAccountInfo implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "id")
    private Integer id;

    @ApiModelProperty(value = "身份证姓名")
    private String idCardName;

    @NotNull
    @ApiModelProperty(value = "用户编号")
    private Integer uid;

    @NotBlank
    @ApiModelProperty(value = "手机号，加密")
    private String phoneNumber;

    @NotBlank
    @ApiModelProperty(value = "邮箱，加密")
    private String email;

    @NotNull
    @ApiModelProperty(value = "totalInvestment")
    private BigDecimal totalInvestment;

    @ApiModelProperty(value = "apiKey，加密")
    private String apiKey;

    @ApiModelProperty(value = "apiSecret，加密")
    private String apiSecret;

    @ApiModelProperty(value = "备注")
    private String remark;

    public void copy(BinanceAccountInfo source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
