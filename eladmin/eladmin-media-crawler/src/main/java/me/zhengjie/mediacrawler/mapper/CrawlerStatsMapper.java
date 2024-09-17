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
package me.zhengjie.mediacrawler.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.zhengjie.mediacrawler.domain.CrawlerCookiesAccount;
import me.zhengjie.mediacrawler.domain.vo.CrawlerTagStats;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author genghui
 * @date 2024-09-08
 **/
@DS("media_crawler")
@Mapper
public interface CrawlerStatsMapper extends BaseMapper<CrawlerCookiesAccount> {

    /**
     * 根据关键词获取相关的tag
     * 按照出现次数倒序排序
     *
     * @param keyword            关键词
     * @param minOccurrenceCount 最小出现次数
     * @param limit              查询数量
     * @return
     */
    List<CrawlerTagStats> getTagListByKeyWord(String keyword, Integer minOccurrenceCount, Integer limit);

    boolean hasCrawled(String keyword);
}