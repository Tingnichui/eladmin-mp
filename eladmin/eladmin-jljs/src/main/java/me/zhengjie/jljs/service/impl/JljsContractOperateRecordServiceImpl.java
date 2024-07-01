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

import me.zhengjie.jljs.domain.JljsContractOperateRecord;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.jljs.service.JljsContractOperateRecordService;
import me.zhengjie.jljs.domain.vo.JljsContractOperateRecordQueryCriteria;
import me.zhengjie.jljs.mapper.JljsContractOperateRecordMapper;
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
* @date 2024-07-02
**/
@Service
@RequiredArgsConstructor
public class JljsContractOperateRecordServiceImpl extends ServiceImpl<JljsContractOperateRecordMapper, JljsContractOperateRecord> implements JljsContractOperateRecordService {

    private final JljsContractOperateRecordMapper jljsContractOperateRecordMapper;

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
        save(resources);
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
}