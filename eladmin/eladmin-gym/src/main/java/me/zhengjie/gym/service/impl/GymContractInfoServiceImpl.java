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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.gym.constants.GymContractStatusEnum;
import me.zhengjie.gym.domain.GymClassRecord;
import me.zhengjie.gym.domain.GymContractInfo;
import me.zhengjie.gym.domain.GymMemberInfo;
import me.zhengjie.gym.service.GymClassRecordService;
import me.zhengjie.gym.service.GymMemberInfoService;
import me.zhengjie.gym.task.SyncGymContractInfoStatusTask;
import me.zhengjie.utils.FileUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.gym.service.GymContractInfoService;
import me.zhengjie.gym.domain.vo.GymContractInfoQueryCriteria;
import me.zhengjie.gym.mapper.GymContractInfoMapper;
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
* @date 2024-06-30
**/
@Service
public class GymContractInfoServiceImpl extends ServiceImpl<GymContractInfoMapper, GymContractInfo> implements GymContractInfoService {

    @Resource
    private GymContractInfoMapper gymContractInfoMapper;
    @Resource
    private GymMemberInfoService gymMemberInfoService;
    @Resource
    private GymClassRecordService gymClassRecordService;
    @Resource
    private SyncGymContractInfoStatusTask syncGymContractInfoStatusTask;

    @Override
    public PageResult<GymContractInfo> queryAll(GymContractInfoQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(gymContractInfoMapper.findAll(criteria, page));
    }

    @Override
    public List<GymContractInfo> queryAll(GymContractInfoQueryCriteria criteria){
        return gymContractInfoMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(GymContractInfo resources) {
        resources.setContractStatus(GymContractStatusEnum.daikaika.getCode());
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(GymContractInfo resources) {
        String id = resources.getId();
        GymContractInfo gymContractInfo = getById(id);
        gymContractInfo.copy(resources);
        saveOrUpdate(gymContractInfo);
        syncGymContractInfoStatusTask.sync(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<String> ids) {
        for (String id : ids) {
            GymContractInfo contractInfo = getById(id);
            if (null == contractInfo) {
                throw new BadRequestException("合同不存在");
            }
            GymMemberInfo memberInfo = gymMemberInfoService.getById(contractInfo.getMemberId());
            String memberName = memberInfo.getMemberName();
            if (!GymContractStatusEnum.daikaika.getCode().equals(contractInfo.getContractStatus())) {
                throw new BadRequestException(memberName + "合同状态不是待开卡，无法删除");
            }
            Long count = gymClassRecordService.getBaseMapper().selectCount(
                    Wrappers.lambdaQuery(GymClassRecord.class)
                            .eq(GymClassRecord::getContractInfoId, id)
            );
            if (count > 0) {
                throw new BadRequestException(memberName + "合同编号[" + contractInfo.getId() + "]下存在" + count + "个上课记录，无法删除");
            }
        }
        removeBatchByIds(ids);
    }

    @Override
    public GymContractInfo getInUseContractInfoByMemberId(String memberId) throws BadRequestException {
        LambdaQueryWrapper<GymContractInfo> wrapper = Wrappers.lambdaQuery(GymContractInfo.class);
        wrapper.eq(GymContractInfo::getMemberId, memberId);
        wrapper.eq(GymContractInfo::getContractStatus, GymContractStatusEnum.shiyong.getCode());
        List<GymContractInfo> list = baseMapper.selectList(wrapper);
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new BadRequestException("该会员存在" + list.size() + "个使用中的合同");
        }
        return list.get(0);
    }

    @Override
    public void download(List<GymContractInfo> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (GymContractInfo gymContractInfo : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("创建人id", gymContractInfo.getCreateBy());
            map.put("创建时间", gymContractInfo.getCreateTime());
            map.put("更新人id", gymContractInfo.getUpdateBy());
            map.put("更新时间", gymContractInfo.getUpdateTime());
            map.put("是否删除;0未删除；秒级时间戳 已删除", gymContractInfo.getDelFlag());
            map.put("会员", gymContractInfo.getMemberId());
            map.put("开单教练", gymContractInfo.getBelongCoachId());
            map.put("合同金额", gymContractInfo.getContractAmount());
            map.put("合同状态", gymContractInfo.getContractStatus());
            map.put("开始日期", gymContractInfo.getUseBeginDate());
            map.put("结束日期", gymContractInfo.getUseEndDate());
            map.put("购买日期", gymContractInfo.getBuyTime());
            map.put("备注", gymContractInfo.getContractRemark());
            map.put("实际收取金额", gymContractInfo.getActualChargeAmount());
            map.put("课程", gymContractInfo.getCourseInfoId());
            map.put("课程类型", gymContractInfo.getCourseType());
            map.put("使用期限", gymContractInfo.getCourseUsePeriodDays());
            map.put("可使用数量", gymContractInfo.getCourseAvailableQuantity());
            map.put("剩余数量", gymContractInfo.getCourseRemainQuantity());
            map.put("已使用数量", gymContractInfo.getCourseUseQuantity());
            map.put("总暂停天数", gymContractInfo.getCourseTotalStopDays());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}