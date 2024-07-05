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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.jljs.constants.JljsContractStatusEnum;
import me.zhengjie.jljs.domain.JljsContractInfo;
import me.zhengjie.jljs.task.SyncContractInfoTask;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.jljs.service.JljsContractInfoService;
import me.zhengjie.jljs.domain.vo.JljsContractInfoQueryCriteria;
import me.zhengjie.jljs.mapper.JljsContractInfoMapper;
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
@RequiredArgsConstructor
public class JljsContractInfoServiceImpl extends ServiceImpl<JljsContractInfoMapper, JljsContractInfo> implements JljsContractInfoService {

    private final JljsContractInfoMapper jljsContractInfoMapper;

    @Resource
    private SyncContractInfoTask syncContractInfoTask;

    @Override
    public PageResult<JljsContractInfo> queryAll(JljsContractInfoQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(jljsContractInfoMapper.findAll(criteria, page));
    }

    @Override
    public List<JljsContractInfo> queryAll(JljsContractInfoQueryCriteria criteria){
        return jljsContractInfoMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(JljsContractInfo resources) {
        resources.setContractStatus(JljsContractStatusEnum.daikaika.getCode());
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(JljsContractInfo resources) {
        String id = resources.getId();
        JljsContractInfo jljsContractInfo = getById(id);
        jljsContractInfo.copy(resources);
        saveOrUpdate(jljsContractInfo);
        syncContractInfoTask.sync(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<String> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public JljsContractInfo getInUseContractInfoByMemberId(String memberId) throws BadRequestException {
        LambdaQueryWrapper<JljsContractInfo> wrapper = Wrappers.lambdaQuery(JljsContractInfo.class);
        wrapper.eq(JljsContractInfo::getMemberId, memberId);
        wrapper.eq(JljsContractInfo::getContractStatus, JljsContractStatusEnum.shiyong.getCode());
        List<JljsContractInfo> list = baseMapper.selectList(wrapper);
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new BadRequestException("该会员存在" + list.size() + "个使用中的合同");
        }
        return list.get(0);
    }

    @Override
    public void download(List<JljsContractInfo> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (JljsContractInfo jljsContractInfo : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("创建人id", jljsContractInfo.getCreateBy());
            map.put("创建时间", jljsContractInfo.getCreateTime());
            map.put("更新人id", jljsContractInfo.getUpdateBy());
            map.put("更新时间", jljsContractInfo.getUpdateTime());
            map.put("是否删除;0未删除；秒级时间戳 已删除", jljsContractInfo.getDelFlag());
            map.put("会员", jljsContractInfo.getMemberId());
            map.put("开单教练", jljsContractInfo.getBelongCoachId());
            map.put("合同金额", jljsContractInfo.getContractAmount());
            map.put("合同状态", jljsContractInfo.getContractStatus());
            map.put("开始日期", jljsContractInfo.getUseBeginDate());
            map.put("结束日期", jljsContractInfo.getUseEndDate());
            map.put("购买日期", jljsContractInfo.getBuyTime());
            map.put("备注", jljsContractInfo.getContractRemark());
            map.put("实际收取金额", jljsContractInfo.getActualChargeAmount());
            map.put("课程", jljsContractInfo.getCourseInfoId());
            map.put("课程类型", jljsContractInfo.getCourseType());
            map.put("使用期限", jljsContractInfo.getCourseUsePeriodDays());
            map.put("可使用数量", jljsContractInfo.getCourseAvailableQuantity());
            map.put("剩余数量", jljsContractInfo.getCourseRemainQuantity());
            map.put("已使用数量", jljsContractInfo.getCourseUseQuantity());
            map.put("总暂停天数", jljsContractInfo.getCourseTotalStopDays());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}