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
package me.zhengjie.invest.domain.dto;

import lombok.Data;
import java.sql.Timestamp;
import java.util.List;

/**
* @author genghui
* @date 2026-06-07
**/
@Data
public class InvestTradeAnalysisQueryCriteria{
    private String direction;
    private String entryType;
    private Integer qualityLevel;
    private List<Timestamp> openTime;
    private List<Timestamp> closeTime;
    private Integer page = 1;
    private Integer size = 10;
}
