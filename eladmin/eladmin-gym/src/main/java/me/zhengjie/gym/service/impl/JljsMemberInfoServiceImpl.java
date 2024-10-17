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

import me.zhengjie.gym.domain.JljsMemberInfo;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.gym.service.JljsMemberInfoService;
import me.zhengjie.gym.domain.vo.JljsMemberInfoQueryCriteria;
import me.zhengjie.gym.mapper.JljsMemberInfoMapper;
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
public class JljsMemberInfoServiceImpl extends ServiceImpl<JljsMemberInfoMapper, JljsMemberInfo> implements JljsMemberInfoService {

    private final JljsMemberInfoMapper jljsMemberInfoMapper;

    @Override
    public PageResult<JljsMemberInfo> queryAll(JljsMemberInfoQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(jljsMemberInfoMapper.findAll(criteria, page));
    }

    @Override
    public List<JljsMemberInfo> queryAll(JljsMemberInfoQueryCriteria criteria){
        return jljsMemberInfoMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(JljsMemberInfo resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(JljsMemberInfo resources) {
        JljsMemberInfo jljsMemberInfo = getById(resources.getId());
        jljsMemberInfo.copy(resources);
        saveOrUpdate(jljsMemberInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<String> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<JljsMemberInfo> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (JljsMemberInfo jljsMemberInfo : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("创建人id", jljsMemberInfo.getCreateBy());
            map.put("创建时间", jljsMemberInfo.getCreateTime());
            map.put("更新人id", jljsMemberInfo.getUpdateBy());
            map.put("更新时间", jljsMemberInfo.getUpdateTime());
            map.put("是否删除;0未删除；秒级时间戳 已删除", jljsMemberInfo.getDelFlag());
            map.put("姓名", jljsMemberInfo.getMemberName());
            map.put("性别", jljsMemberInfo.getMemberGender());
            map.put("年龄", jljsMemberInfo.getMemberAge());
            map.put("手机号", jljsMemberInfo.getMemberPhoneNum());
            map.put("生日", jljsMemberInfo.getBirthDay());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}