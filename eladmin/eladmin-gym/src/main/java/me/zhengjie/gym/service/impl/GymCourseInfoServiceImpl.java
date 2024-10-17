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

import me.zhengjie.gym.domain.GymCourseInfo;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.gym.service.GymCourseInfoService;
import me.zhengjie.gym.domain.vo.GymCourseInfoQueryCriteria;
import me.zhengjie.gym.mapper.GymCourseInfoMapper;
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
public class GymCourseInfoServiceImpl extends ServiceImpl<GymCourseInfoMapper, GymCourseInfo> implements GymCourseInfoService {

    private final GymCourseInfoMapper gymCourseInfoMapper;

    @Override
    public PageResult<GymCourseInfo> queryAll(GymCourseInfoQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(gymCourseInfoMapper.findAll(criteria, page));
    }

    @Override
    public List<GymCourseInfo> queryAll(GymCourseInfoQueryCriteria criteria){
        return gymCourseInfoMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(GymCourseInfo resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(GymCourseInfo resources) {
        GymCourseInfo gymCourseInfo = getById(resources.getId());
        gymCourseInfo.copy(resources);
        saveOrUpdate(gymCourseInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<String> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<GymCourseInfo> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (GymCourseInfo gymCourseInfo : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("创建人id", gymCourseInfo.getCreateBy());
            map.put("创建时间", gymCourseInfo.getCreateTime());
            map.put("更新人id", gymCourseInfo.getUpdateBy());
            map.put("更新时间", gymCourseInfo.getUpdateTime());
            map.put("是否删除;0未删除；秒级时间戳 已删除", gymCourseInfo.getDelFlag());
            map.put("名称", gymCourseInfo.getCourseName());
            map.put("价格", gymCourseInfo.getCoursePrice());
            map.put("描述", gymCourseInfo.getCourseDescribe());
            map.put("类型", gymCourseInfo.getCourseType());
            map.put("使用期限", gymCourseInfo.getCourseUsePeriodDays());
            map.put("可使用数量", gymCourseInfo.getCourseAvailableQuantity());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}