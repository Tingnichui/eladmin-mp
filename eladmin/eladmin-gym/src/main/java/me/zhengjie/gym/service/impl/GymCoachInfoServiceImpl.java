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

import me.zhengjie.gym.domain.GymCoachInfo;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.gym.service.GymCoachInfoService;
import me.zhengjie.gym.domain.vo.GymCoachInfoQueryCriteria;
import me.zhengjie.gym.mapper.GymCoachInfoMapper;
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
public class GymCoachInfoServiceImpl extends ServiceImpl<GymCoachInfoMapper, GymCoachInfo> implements GymCoachInfoService {

    private final GymCoachInfoMapper gymCoachInfoMapper;

    @Override
    public PageResult<GymCoachInfo> queryAll(GymCoachInfoQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(gymCoachInfoMapper.findAll(criteria, page));
    }

    @Override
    public List<GymCoachInfo> queryAll(GymCoachInfoQueryCriteria criteria){
        return gymCoachInfoMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(GymCoachInfo resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(GymCoachInfo resources) {
        GymCoachInfo gymCoachInfo = getById(resources.getId());
        gymCoachInfo.copy(resources);
        saveOrUpdate(gymCoachInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<String> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<GymCoachInfo> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (GymCoachInfo gymCoachInfo : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("创建人id", gymCoachInfo.getCreateBy());
            map.put("创建时间", gymCoachInfo.getCreateTime());
            map.put("更新人id", gymCoachInfo.getUpdateBy());
            map.put("更新时间", gymCoachInfo.getUpdateTime());
            map.put("是否删除;0未删除；秒级时间戳 已删除", gymCoachInfo.getDelFlag());
            map.put("教练姓名", gymCoachInfo.getCoachName());
            map.put("电话号码", gymCoachInfo.getCoachPhoneNum());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}