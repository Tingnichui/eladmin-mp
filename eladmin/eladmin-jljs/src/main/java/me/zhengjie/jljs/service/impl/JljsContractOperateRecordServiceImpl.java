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
package me.zhengjie.jljs.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.jljs.constants.JljsContractOperateTypeEnum;
import me.zhengjie.jljs.constants.JljsContractStatusEnum;
import me.zhengjie.jljs.constants.JljsOperateStatusEnum;
import me.zhengjie.jljs.domain.JljsContractInfo;
import me.zhengjie.jljs.domain.JljsContractOperateRecord;
import me.zhengjie.jljs.mapper.JljsContractInfoMapper;
import me.zhengjie.jljs.service.JljsContractInfoService;
import me.zhengjie.jljs.task.SyncContractInfoTask;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.jljs.service.JljsContractOperateRecordService;
import me.zhengjie.jljs.domain.vo.JljsContractOperateRecordQueryCriteria;
import me.zhengjie.jljs.mapper.JljsContractOperateRecordMapper;
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
public class JljsContractOperateRecordServiceImpl extends ServiceImpl<JljsContractOperateRecordMapper, JljsContractOperateRecord> implements JljsContractOperateRecordService {

    private final JljsContractOperateRecordMapper jljsContractOperateRecordMapper;
    private final SyncContractInfoTask syncContractInfoTask;
    private final JljsContractInfoMapper jljsContractInfoDao;
    private final JljsContractInfoService jljsContractInfoService;

    @Override
    public PageResult<JljsContractOperateRecord> queryAll(JljsContractOperateRecordQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(jljsContractOperateRecordMapper.findAll(criteria, page));
    }

    @Override
    public List<JljsContractOperateRecord> queryAll(JljsContractOperateRecordQueryCriteria criteria){
        return jljsContractOperateRecordMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(JljsContractOperateRecord resources) {
        JljsContractInfo contractInfo = jljsContractInfoDao.selectById(resources.getContractInfoId());
        if (null == contractInfo) {
            throw new BadRequestException("合同不存在");
        }

        try {
            String contractStatus = contractInfo.getContractStatus();
            String operateType = resources.getContractOperateType();
            // 开卡 更新合同开始日期
            if (JljsContractOperateTypeEnum.kaika.getCode().equals(operateType)) {
                if (!JljsContractStatusEnum.daikaika.getCode().equals(contractStatus)) {
                    throw new BadRequestException("合同不是待开卡状态，不可操作");
                }
                // 判断存在几个使用中的合同
                JljsContractInfo contractInfoInUse = jljsContractInfoService.getInUseContractInfoByMemberId(contractInfo.getMemberId());
                if (null != contractInfoInUse) {
                    throw new BadRequestException("已经存在正在使用的合同,不可再次开卡");
                }
                if (null == resources.getOperateBeginDate()) {
                    throw new BadRequestException("开卡时间不能为空");
                }
                LambdaUpdateWrapper<JljsContractInfo> updateWrapper = Wrappers.lambdaUpdate(JljsContractInfo.class);
                updateWrapper.eq(JljsContractInfo::getId, contractInfo.getId());
                updateWrapper.set(JljsContractInfo::getUseBeginDate, resources.getOperateBeginDate());
                updateWrapper.set(JljsContractInfo::getContractStatus, JljsContractStatusEnum.shiyong.getCode());
                jljsContractInfoDao.update(null, updateWrapper);
                // 保存操作记录
                this.save(resources);
            }
            // 暂停 计算间隔时间 更新合同剩余有效天数/有效次数
            if (JljsContractOperateTypeEnum.zanting.getCode().equals(operateType)) {
                if (!StringUtils.equalsAny(contractStatus, JljsContractStatusEnum.shiyong.getCode(), JljsContractStatusEnum.zanting.getCode())) {
                    throw new BadRequestException("只有使用中或者暂停中的合同才可以进行停课");
                }
                if (ObjectUtils.anyNull(resources.getOperateBeginDate(), resources.getOperateEndDate())) {
                    throw new BadRequestException("起始时间不能为空");
                }
                // 获取该合同所有的成功的请假记录
                List<JljsContractOperateRecord> list = baseMapper.selectList(
                        Wrappers.lambdaQuery(JljsContractOperateRecord.class)
                                .eq(JljsContractOperateRecord::getContractInfoId, contractInfo.getId())
                                .eq(JljsContractOperateRecord::getOperateStatus, JljsOperateStatusEnum.chenggong.getCode())
                                .eq(JljsContractOperateRecord::getContractOperateType, JljsContractOperateTypeEnum.zanting.getCode())
                );
                if (resources.getOperateBeginDate().before(contractInfo.getUseBeginDate())) {
                    throw new BadRequestException("请假时间不能早于合同开始时间");
                }
                // 判断请假时间在不在合同有效期内
                if (!DateUtil.isIn(resources.getOperateBeginDate(), contractInfo.getUseBeginDate(), contractInfo.getUseEndDate())
                        && !DateUtil.isIn(resources.getOperateEndDate(), contractInfo.getUseBeginDate(), contractInfo.getUseEndDate())) {
                    throw new BadRequestException("请假时间需要在合同起始时间内");
                }
                for (JljsContractOperateRecord record : list) {
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
            if (JljsContractOperateTypeEnum.tuike.getCode().equals(operateType)) {
                if (StringUtils.equalsAny(contractStatus, JljsContractStatusEnum.wancheng.getCode(), JljsContractStatusEnum.zhongzhi.getCode())) {
                    throw new BadRequestException("合同已完成或已终止，不可操作");
                }
                if (null == resources.getOperateAmount()) {
                    throw new BadRequestException("退课金额不能为空");
                }
                // 保存操作记录
                this.save(resources);
                // 更新合同状态 终止
                LambdaUpdateWrapper<JljsContractInfo> updateWrapper = Wrappers.lambdaUpdate(JljsContractInfo.class);
                updateWrapper.eq(JljsContractInfo::getId, contractInfo.getId());
                updateWrapper.set(JljsContractInfo::getContractStatus, JljsContractStatusEnum.zhongzhi.getCode());
                jljsContractInfoDao.update(null, updateWrapper);
            }
            // 补缴
            if (JljsContractOperateTypeEnum.bujiao.getCode().equals(operateType)) {
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
    public void update(JljsContractOperateRecord resources) {
        JljsContractOperateRecord jljsContractOperateRecord = getById(resources.getId());
        jljsContractOperateRecord.copy(resources);
        saveOrUpdate(jljsContractOperateRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<String> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<JljsContractOperateRecord> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (JljsContractOperateRecord jljsContractOperateRecord : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("创建人id", jljsContractOperateRecord.getCreateBy());
            map.put("创建时间", jljsContractOperateRecord.getCreateTime());
            map.put("更新人id", jljsContractOperateRecord.getUpdateBy());
            map.put("更新时间", jljsContractOperateRecord.getUpdateTime());
            map.put("是否删除;0未删除；秒级时间戳 已删除", jljsContractOperateRecord.getDelFlag());
            map.put("合同id", jljsContractOperateRecord.getContractInfoId());
            map.put("合同操作类型", jljsContractOperateRecord.getContractOperateType());
            map.put("间隔天数", jljsContractOperateRecord.getIntervalDays());
            map.put("开始时间", jljsContractOperateRecord.getOperateBeginDate());
            map.put("结束时间", jljsContractOperateRecord.getOperateEndDate());
            map.put("操作原因", jljsContractOperateRecord.getOperateReason());
            map.put("操作金额", jljsContractOperateRecord.getOperateAmount());
            map.put("状态", jljsContractOperateRecord.getOperateStatus());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(String id) {
        JljsContractOperateRecord record = baseMapper.selectById(id);
        if (null == record) {
            throw  new BadRequestException("操作记录不存在");
        }
        // 更新为撤销状态
        record.setOperateStatus(JljsOperateStatusEnum.chexiao.getCode());
        baseMapper.updateById(record);

        // 撤销开卡 更新合同为待开卡 使用日期置空
        if (JljsContractOperateTypeEnum.kaika.getCode().equals(record.getContractOperateType())) {
            LambdaUpdateWrapper<JljsContractInfo> update = Wrappers.lambdaUpdate(JljsContractInfo.class);
            update.eq(JljsContractInfo::getId, record.getContractInfoId());
            update.set(JljsContractInfo::getContractStatus, JljsContractStatusEnum.daikaika.getCode());
            update.set(JljsContractInfo::getUseBeginDate, null);
            jljsContractInfoDao.update(null, update);
        }

        syncContractInfoTask.sync(record.getContractInfoId());
    }
}