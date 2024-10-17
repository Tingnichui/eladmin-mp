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

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.gym.constants.GymContractOperateTypeEnum;
import me.zhengjie.gym.constants.GymContractStatusEnum;
import me.zhengjie.gym.constants.GymContractOperateStatusEnum;
import me.zhengjie.gym.domain.GymContractInfo;
import me.zhengjie.gym.domain.GymContractOperateRecord;
import me.zhengjie.gym.mapper.GymContractInfoMapper;
import me.zhengjie.gym.service.GymContractInfoService;
import me.zhengjie.gym.task.SyncContractInfoTask;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.gym.service.GymContractOperateRecordService;
import me.zhengjie.gym.domain.vo.GymContractOperateRecordQueryCriteria;
import me.zhengjie.gym.mapper.GymContractOperateRecordMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.zhengjie.utils.PageUtil;

import java.util.*;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import me.zhengjie.utils.PageResult;

/**
* @description 服务实现
* @author genghui
* @date 2024-07-02
**/
@Service
@RequiredArgsConstructor
public class GymContractOperateRecordServiceImpl extends ServiceImpl<GymContractOperateRecordMapper, GymContractOperateRecord> implements GymContractOperateRecordService {

    private final GymContractOperateRecordMapper gymContractOperateRecordMapper;
    private final SyncContractInfoTask syncContractInfoTask;
    private final GymContractInfoMapper jljsContractInfoDao;
    private final GymContractInfoService gymContractInfoService;

    @Override
    public PageResult<GymContractOperateRecord> queryAll(GymContractOperateRecordQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(gymContractOperateRecordMapper.findAll(criteria, page));
    }

    @Override
    public List<GymContractOperateRecord> queryAll(GymContractOperateRecordQueryCriteria criteria){
        return gymContractOperateRecordMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(GymContractOperateRecord resources) {
        GymContractInfo contractInfo = jljsContractInfoDao.selectById(resources.getContractInfoId());
        if (null == contractInfo) {
            throw new BadRequestException("合同不存在");
        }

        try {
            String contractStatus = contractInfo.getContractStatus();
            String operateType = resources.getContractOperateType();
            // 开卡 更新合同开始日期
            if (GymContractOperateTypeEnum.kaika.getCode().equals(operateType)) {
                if (!GymContractStatusEnum.daikaika.getCode().equals(contractStatus)) {
                    throw new BadRequestException("合同不是待开卡状态，不可操作");
                }
                // 判断存在几个使用中的合同
                GymContractInfo contractInfoInUse = gymContractInfoService.getInUseContractInfoByMemberId(contractInfo.getMemberId());
                if (null != contractInfoInUse) {
                    throw new BadRequestException("已经存在正在使用的合同,不可再次开卡");
                }
                if (null == resources.getOperateBeginDate()) {
                    throw new BadRequestException("开卡时间不能为空");
                }
                LambdaUpdateWrapper<GymContractInfo> updateWrapper = Wrappers.lambdaUpdate(GymContractInfo.class);
                updateWrapper.eq(GymContractInfo::getId, contractInfo.getId());
                updateWrapper.set(GymContractInfo::getUseBeginDate, resources.getOperateBeginDate());
                updateWrapper.set(GymContractInfo::getContractStatus, GymContractStatusEnum.shiyong.getCode());
                jljsContractInfoDao.update(null, updateWrapper);
                // 保存操作记录
                this.save(resources);
            }
            // 暂停 计算间隔时间 更新合同剩余有效天数/有效次数
            if (GymContractOperateTypeEnum.zanting.getCode().equals(operateType)) {
                if (!StringUtils.equalsAny(contractStatus, GymContractStatusEnum.shiyong.getCode(), GymContractStatusEnum.zanting.getCode())) {
                    throw new BadRequestException("只有使用中或者暂停中的合同才可以进行停课");
                }
                if (ObjectUtils.anyNull(resources.getOperateBeginDate(), resources.getOperateEndDate())) {
                    throw new BadRequestException("起始时间不能为空");
                }
                // 获取该合同所有的成功的请假记录
                List<GymContractOperateRecord> list = baseMapper.selectList(
                        Wrappers.lambdaQuery(GymContractOperateRecord.class)
                                .eq(GymContractOperateRecord::getContractInfoId, contractInfo.getId())
                                .eq(GymContractOperateRecord::getOperateStatus, GymContractOperateStatusEnum.chenggong.getCode())
                                .eq(GymContractOperateRecord::getContractOperateType, GymContractOperateTypeEnum.zanting.getCode())
                );
                if (resources.getOperateBeginDate().before(contractInfo.getUseBeginDate())) {
                    throw new BadRequestException("请假时间不能早于合同开始时间");
                }
                // 判断请假时间在不在合同有效期内
                if (!DateUtil.isIn(resources.getOperateBeginDate(), contractInfo.getUseBeginDate(), contractInfo.getUseEndDate())
                        && !DateUtil.isIn(resources.getOperateEndDate(), contractInfo.getUseBeginDate(), contractInfo.getUseEndDate())) {
                    throw new BadRequestException("请假时间需要在合同起始时间内");
                }
                for (GymContractOperateRecord record : list) {
                    Date operateBeginDate = record.getOperateBeginDate();
                    Date operateEndDate = record.getOperateEndDate();
                    // 请假起始时间在之前的请假记录内 或者 之前的请假起始时间包含了请假的时间
                    if (DateUtil.isIn(resources.getOperateBeginDate(), operateBeginDate, operateEndDate)
                            || DateUtil.isIn(resources.getOperateEndDate(), operateBeginDate, operateEndDate)
                            || DateUtil.isIn(operateBeginDate, resources.getOperateBeginDate(), resources.getOperateEndDate())
                            || DateUtil.isIn(operateEndDate, resources.getOperateBeginDate(), resources.getOperateEndDate())) {
                        throw new BadRequestException("已存在" + DateUtil.formatDate(operateBeginDate) + "~" + DateUtil.formatDate(operateEndDate) + "请假记录，时间不可重复");
                    }
                }

                long dayDiff = DateUtil.betweenDay(resources.getOperateBeginDate(), resources.getOperateEndDate(), true) + 1;
                resources.setIntervalDays((int) dayDiff);
                // 保存操作记录
                this.save(resources);
            }
            // 退课
            if (GymContractOperateTypeEnum.tuike.getCode().equals(operateType)) {
                if (StringUtils.equalsAny(contractStatus, GymContractStatusEnum.wancheng.getCode(), GymContractStatusEnum.zhongzhi.getCode())) {
                    throw new BadRequestException("合同已完成或已终止，不可操作");
                }
                if (null == resources.getOperateAmount()) {
                    throw new BadRequestException("退课金额不能为空");
                }
                // 保存操作记录
                this.save(resources);
                // 更新合同状态 终止
                LambdaUpdateWrapper<GymContractInfo> updateWrapper = Wrappers.lambdaUpdate(GymContractInfo.class);
                updateWrapper.eq(GymContractInfo::getId, contractInfo.getId());
                updateWrapper.set(GymContractInfo::getContractStatus, GymContractStatusEnum.zhongzhi.getCode());
                jljsContractInfoDao.update(null, updateWrapper);
            }
            // 补缴
            if (GymContractOperateTypeEnum.bujiao.getCode().equals(operateType)) {
                if (null == resources.getOperateAmount()) {
                    throw new BadRequestException("补缴金额不能为空");
                }
                this.save(resources);
            }
        } finally {
            // 更新合同信息
            syncContractInfoTask.sync(resources.getContractInfoId());
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(GymContractOperateRecord resources) {
        GymContractOperateRecord gymContractOperateRecord = getById(resources.getId());
        gymContractOperateRecord.copy(resources);
        saveOrUpdate(gymContractOperateRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<String> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<GymContractOperateRecord> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (GymContractOperateRecord gymContractOperateRecord : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("创建人id", gymContractOperateRecord.getCreateBy());
            map.put("创建时间", gymContractOperateRecord.getCreateTime());
            map.put("更新人id", gymContractOperateRecord.getUpdateBy());
            map.put("更新时间", gymContractOperateRecord.getUpdateTime());
            map.put("是否删除;0未删除；秒级时间戳 已删除", gymContractOperateRecord.getDelFlag());
            map.put("合同id", gymContractOperateRecord.getContractInfoId());
            map.put("合同操作类型", gymContractOperateRecord.getContractOperateType());
            map.put("间隔天数", gymContractOperateRecord.getIntervalDays());
            map.put("开始时间", gymContractOperateRecord.getOperateBeginDate());
            map.put("结束时间", gymContractOperateRecord.getOperateEndDate());
            map.put("操作原因", gymContractOperateRecord.getOperateReason());
            map.put("操作金额", gymContractOperateRecord.getOperateAmount());
            map.put("状态", gymContractOperateRecord.getOperateStatus());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(String id) {
        GymContractOperateRecord record = baseMapper.selectById(id);
        if (null == record) {
            throw  new BadRequestException("操作记录不存在");
        }
        // 更新为撤销状态
        record.setOperateStatus(GymContractOperateStatusEnum.chexiao.getCode());
        baseMapper.updateById(record);

        // 撤销开卡 更新合同为待开卡 使用日期置空
        if (GymContractOperateTypeEnum.kaika.getCode().equals(record.getContractOperateType())) {
            LambdaUpdateWrapper<GymContractInfo> update = Wrappers.lambdaUpdate(GymContractInfo.class);
            update.eq(GymContractInfo::getId, record.getContractInfoId());
            update.set(GymContractInfo::getContractStatus, GymContractStatusEnum.daikaika.getCode());
            update.set(GymContractInfo::getUseBeginDate, null);
            jljsContractInfoDao.update(null, update);
        }

        syncContractInfoTask.sync(record.getContractInfoId());
    }
}