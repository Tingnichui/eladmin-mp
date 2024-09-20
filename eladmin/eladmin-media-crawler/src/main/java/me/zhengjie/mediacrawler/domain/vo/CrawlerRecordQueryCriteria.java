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
package me.zhengjie.mediacrawler.domain.vo;

import lombok.Data;

/**
* @author genghui
* @date 2024-09-20
**/
@Data
public class CrawlerRecordQueryCriteria{
    private String platform;
    private String crawlerType;
    private String keywords;
    private String crawlerStatus;
}