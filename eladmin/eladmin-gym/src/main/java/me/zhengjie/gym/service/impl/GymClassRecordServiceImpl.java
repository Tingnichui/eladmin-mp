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

import me.zhengjie.exception.BadRequestException;
import me.zhengjie.gym.domain.GymClassRecord;
import me.zhengjie.gym.domain.GymContractInfo;
import me.zhengjie.gym.service.GymContractInfoService;
import me.zhengjie.gym.task.SyncContractInfoTask;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.gym.service.GymClassRecordService;
import me.zhengjie.gym.domain.vo.GymClassRecordQueryCriteria;
import me.zhengjie.gym.mapper.GymClassRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.zhengjie.utils.PageUtil;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import me.zhengjie.utils.PageResult;

/**
* @description 服务实现
* @author genghui
* @date 2024-07-02
**/
@Service
@RequiredArgsConstructor
public class GymClassRecordServiceImpl extends ServiceImpl<GymClassRecordMapper, GymClassRecord> implements GymClassRecordService {

    @Resource
    private GymClassRecordMapper gymClassRecordMapper;
    @Resource
    private GymContractInfoService gymContractInfoService;
    @Resource
    private SyncContractInfoTask syncContractInfoTask;

    @Override
    public PageResult<GymClassRecord> queryAll(GymClassRecordQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(gymClassRecordMapper.findAll(criteria, page));
    }

    @Override
    public List<GymClassRecord> queryAll(GymClassRecordQueryCriteria criteria){
        return gymClassRecordMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(GymClassRecord resources) {
        if (resources.getClassBeginTime().after(resources.getClassEndTime())) {
            throw new BadRequestException("开始时间不能早于结束时间");
        }
        // 判断该会员是否有正常使用的合同
        GymContractInfo contractInfo = gymContractInfoService.getInUseContractInfoByMemberId(resources.getMemberId());
        if (null == contractInfo) {
            throw new BadRequestException("该会员没有使用中的合同");
        }
        String contractInfoId = contractInfo.getId();
        resources.setContractInfoId(contractInfoId);
        save(resources);
        // 更新合同信息
        syncContractInfoTask.sync(contractInfoId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(GymClassRecord resources) {
        if (resources.getClassBeginTime().after(resources.getClassEndTime())) {
            throw new BadRequestException("开始时间不能早于结束时间");
        }
        GymClassRecord gymClassRecord = getById(resources.getId());
        gymClassRecord.copy(resources);
        // 判断该会员是否有正常使用的合同
        GymContractInfo contractInfo = gymContractInfoService.getInUseContractInfoByMemberId(resources.getMemberId());
        if (null == contractInfo) {
            throw new BadRequestException("该会员没有使用中的合同");
        }
        String contractInfoId = contractInfo.getId();
        gymClassRecord.setContractInfoId(contractInfoId);
        saveOrUpdate(gymClassRecord);
        // 更新合同信息
        syncContractInfoTask.sync(contractInfoId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<String> ids) {
        removeBatchByIds(ids);
        // 更新合同信息
        for (String id : ids) {
            syncContractInfoTask.sync(id);
        }
    }

    @Override
    public void download(List<GymClassRecord> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (GymClassRecord gymClassRecord : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("创建人id", gymClassRecord.getCreateBy());
            map.put("创建时间", gymClassRecord.getCreateTime());
            map.put("更新人id", gymClassRecord.getUpdateBy());
            map.put("更新时间", gymClassRecord.getUpdateTime());
            map.put("是否删除;0未删除；秒级时间戳 已删除", gymClassRecord.getDelFlag());
            map.put("教练id", gymClassRecord.getCoachId());
            map.put("会员id", gymClassRecord.getMemberId());
            map.put("关联合同id", gymClassRecord.getContractInfoId());
            map.put("课程开始时间", gymClassRecord.getClassBeginTime());
            map.put("课程结束时间", gymClassRecord.getClassEndTime());
            map.put("课程备注", gymClassRecord.getClassRemark());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}