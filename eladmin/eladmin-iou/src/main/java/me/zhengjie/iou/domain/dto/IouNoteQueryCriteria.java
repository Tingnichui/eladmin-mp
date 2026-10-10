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
package me.zhengjie.iou.domain.dto;

import lombok.Data;
import java.sql.Timestamp;
import java.util.List;
import io.swagger.annotations.ApiModelProperty;

/**
* @author chunhui
* @date 2026-10-10
**/
@Data
public class IouNoteQueryCriteria{

    @ApiModelProperty(value = "页码", example = "1")
    private Integer page = 1;

    @ApiModelProperty(value = "每页数据量", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "借条编号")
    private String noteNo;

    @ApiModelProperty(value = "发起人ID")
    private String creatorUserId;

    @ApiModelProperty(value = "借款人")
    private String borrowerName;

    @ApiModelProperty(value = "出借人")
    private String lenderName;

    @ApiModelProperty(value = "借款类型")
    private Integer loanType;

    @ApiModelProperty(value = "借条状态")
    private Integer status;
    private List<Timestamp> dueDate;
    private List<Timestamp> createTime;
}
