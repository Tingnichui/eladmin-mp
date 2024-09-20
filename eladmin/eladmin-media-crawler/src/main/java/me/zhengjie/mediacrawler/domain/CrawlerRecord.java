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
package me.zhengjie.mediacrawler.domain;

import lombok.Data;
import cn.hutool.core.bean.BeanUtil;
import io.swagger.annotations.ApiModelProperty;
import cn.hutool.core.bean.copier.CopyOptions;
import java.sql.Timestamp;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
* @description /
* @author genghui
* @date 2024-09-20
**/
@Data
@TableName("crawler_record")
public class CrawlerRecord implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "id")
    private Integer id;

    @NotBlank
    @ApiModelProperty(value = "自媒体平台")
    private String platform;

    @NotBlank
    @ApiModelProperty(value = "爬取类型")
    private String crawlerType;

    @ApiModelProperty(value = "关键词")
    private String keywords;

    @NotNull
    @ApiModelProperty(value = "开始页数")
    private Integer startPage;

    @ApiModelProperty(value = "结束页数")
    private Integer endPage;

    @NotBlank
    @ApiModelProperty(value = "爬虫状态")
    private String crawlerStatus;

    @ApiModelProperty(value = "异常信息")
    private String errorMsg;

    @ApiModelProperty(value = "日志路径")
    private String logPath;

    @ApiModelProperty(value = "开始时间")
    private Timestamp startTime;

    @ApiModelProperty(value = "结束时间")
    private Timestamp endTime;

    @ApiModelProperty(value = "创建者")
    private String createBy;

    @ApiModelProperty(value = "更新者")
    private String updateBy;

    @ApiModelProperty(value = "创建日期")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    public void copy(CrawlerRecord source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
