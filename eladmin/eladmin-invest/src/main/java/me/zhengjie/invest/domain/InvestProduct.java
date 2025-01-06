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
import java.sql.Timestamp;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import me.zhengjie.base.BaseEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
* @description /
* @author genghui
* @date 2025-01-04
**/
@Data
@TableName("invest_product")
public class InvestProduct extends BaseEntity implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "id")
    private Integer id;

    @NotNull
    @ApiModelProperty(value = "投资类型；1股票；2期货；3加密货币")
    private Integer investType;

    @ApiModelProperty(value = "投资产品编码")
    private String investProductCode;

    @NotBlank
    @ApiModelProperty(value = "投资产品名称")
    private String investProductName;

    @NotNull
    @ApiModelProperty(value = "最小购买单位；例如最小购买0.001，那么这边设置为1000")
    private Integer minSize;

    @NotBlank
    @ApiModelProperty(value = "计量单位")
    private String measurementUnit;

    public void copy(InvestProduct source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
