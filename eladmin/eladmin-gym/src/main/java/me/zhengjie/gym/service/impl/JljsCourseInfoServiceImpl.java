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
package me.zhengjie.gym.service.impl;

import me.zhengjie.gym.domain.JljsCourseInfo;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.gym.service.JljsCourseInfoService;
import me.zhengjie.gym.domain.vo.JljsCourseInfoQueryCriteria;
import me.zhengjie.gym.mapper.JljsCourseInfoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.zhengjie.utils.PageUtil;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import me.zhengjie.utils.PageResult;

/**
* @description 服务实现
* @author genghui
* @date 2024-06-30
**/
@Service
@RequiredArgsConstructor
public class JljsCourseInfoServiceImpl extends ServiceImpl<JljsCourseInfoMapper, JljsCourseInfo> implements JljsCourseInfoService {

    private final JljsCourseInfoMapper jljsCourseInfoMapper;

    @Override
    public PageResult<JljsCourseInfo> queryAll(JljsCourseInfoQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(jljsCourseInfoMapper.findAll(criteria, page));
    }

    @Override
    public List<JljsCourseInfo> queryAll(JljsCourseInfoQueryCriteria criteria){
        return jljsCourseInfoMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(JljsCourseInfo resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(JljsCourseInfo resources) {
        JljsCourseInfo jljsCourseInfo = getById(resources.getId());
        jljsCourseInfo.copy(resources);
        saveOrUpdate(jljsCourseInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<String> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<JljsCourseInfo> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (JljsCourseInfo jljsCourseInfo : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("创建人id", jljsCourseInfo.getCreateBy());
            map.put("创建时间", jljsCourseInfo.getCreateTime());
            map.put("更新人id", jljsCourseInfo.getUpdateBy());
            map.put("更新时间", jljsCourseInfo.getUpdateTime());
            map.put("是否删除;0未删除；秒级时间戳 已删除", jljsCourseInfo.getDelFlag());
            map.put("名称", jljsCourseInfo.getCourseName());
            map.put("价格", jljsCourseInfo.getCoursePrice());
            map.put("描述", jljsCourseInfo.getCourseDescribe());
            map.put("类型", jljsCourseInfo.getCourseType());
            map.put("使用期限", jljsCourseInfo.getCourseUsePeriodDays());
            map.put("可使用数量", jljsCourseInfo.getCourseAvailableQuantity());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}