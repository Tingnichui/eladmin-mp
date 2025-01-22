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
package me.zhengjie.invest.domain.vo;

import lombok.Data;

import javax.validation.constraints.Pattern;
import java.sql.Timestamp;
import java.util.List;

/**
* @author genghui
* @date 2025-01-04
**/
@Data
public class InvestTradeRecordQueryCriteria{
    private Integer id;
    private Integer tradeType;
    private Integer operateStatus;
    private Integer productId;
    private List<Timestamp> openTime;
    private Integer score;
    @Pattern(regexp = "|profit")
    private String sortField;
    @Pattern(regexp = "|asc|desc")
    private String sortOrder;
}