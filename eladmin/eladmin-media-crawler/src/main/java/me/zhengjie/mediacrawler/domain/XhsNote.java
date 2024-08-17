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
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
* @description /
* @author genghui
* @date 2024-08-17
**/
@Data
@TableName("xhs_note")
public class XhsNote implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "自增ID")
    private Integer id;

    @NotBlank
    @ApiModelProperty(value = "用户ID")
    private String userId;

    @ApiModelProperty(value = "用户昵称")
    private String nickname;

    @ApiModelProperty(value = "用户头像地址")
    private String avatar;

    @ApiModelProperty(value = "评论时的IP地址")
    private String ipLocation;

    @NotNull
    @ApiModelProperty(value = "记录添加时间戳")
    private Long addTs;

    @NotNull
    @ApiModelProperty(value = "记录最后修改时间戳")
    private Long lastModifyTs;

    @NotBlank
    @ApiModelProperty(value = "笔记ID")
    private String noteId;

    @ApiModelProperty(value = "笔记类型(normal | video)")
    private String type;

    @ApiModelProperty(value = "笔记标题")
    private String title;

    @ApiModelProperty(value = "笔记描述")
    private String desc;

    @ApiModelProperty(value = "视频地址")
    private String videoUrl;

    @NotNull
    @ApiModelProperty(value = "笔记发布时间戳")
    private Long time;

    @NotNull
    @ApiModelProperty(value = "笔记最后更新时间戳")
    private Long lastUpdateTime;

    @ApiModelProperty(value = "笔记点赞数")
    private String likedCount;

    @ApiModelProperty(value = "笔记收藏数")
    private String collectedCount;

    @ApiModelProperty(value = "笔记评论数")
    private String commentCount;

    @ApiModelProperty(value = "笔记分享数")
    private String shareCount;

    @ApiModelProperty(value = "笔记封面图片列表")
    private String imageList;

    @ApiModelProperty(value = "标签列表")
    private String tagList;

    @ApiModelProperty(value = "笔记详情页的URL")
    private String noteUrl;

    public void copy(XhsNote source){
        BeanUtil.copyProperties(source,this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
