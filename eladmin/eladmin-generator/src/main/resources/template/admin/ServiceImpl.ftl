/*
*  Copyright 2019-2025 Zheng Jie
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
package ${package}.service.impl;

import ${package}.domain.${className};
import me.zhengjie.utils.FileUtil;
<#if hasDict>
import me.zhengjie.utils.RedisUtils;
</#if>
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ${package}.service.${className}Service;
import ${package}.domain.dto.${className}QueryCriteria;
import ${package}.mapper.${className}Mapper;
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
* @author ${author}
* @date ${date}
**/
@Service
@RequiredArgsConstructor
public class ${className}ServiceImpl extends ServiceImpl<${className}Mapper, ${className}> implements ${className}Service {

    private final ${className}Mapper ${changeClassName}Mapper;
<#if hasDict>
    private final RedisUtils redisUtils;
</#if>

    @Override
    public PageResult<${className}> queryAll(${className}QueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(${changeClassName}Mapper.findAll(criteria, page));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(${className} resources) {
        ${changeClassName}Mapper.insert(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(${className} resources) {
        ${className} ${changeClassName} = getById(resources.get${pkCapitalColName}());
        ${changeClassName}.copy(resources);
        ${changeClassName}Mapper.updateById(${changeClassName});
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<${pkColumnType}> ids) {
        ${changeClassName}Mapper.deleteBatchIds(ids);
    }

    @Override
    public void download(${className}QueryCriteria criteria, HttpServletResponse response) throws IOException {
        List<String> headers = new ArrayList<>();
    <#list columns as column>
        <#if (column.columnKey!'') != 'PRI'>
        headers.add("${column.exportName}");
        </#if>
    </#list>
        FileUtil.downloadExcel(headers, response, writer -> {
            long current = 1L;
            final long pageSize = 10000L;
            while (true) {
                Page<Object> page = new Page<>(current, pageSize, false);
                List<${className}> records = ${changeClassName}Mapper.findAll(criteria, page).getRecords();
                if (records.isEmpty()) {
                    break;
                }
                List<Map<String, Object>> list = new ArrayList<>(records.size());
                for (${className} ${changeClassName} : records) {
                    Map<String,Object> map = new LinkedHashMap<>();
                <#list columns as column>
                    <#if (column.columnKey!'') != 'PRI'>
                    <#if (column.dictName)?? && (column.dictName)!="">
                    map.put("${column.exportName}", getDictLabel("${column.dictName}", ${changeClassName}.get${column.capitalColumnName}()));
                    <#else>
                    <#if column.longType>
                    map.put("${column.exportName}", getExportValue(${changeClassName}.get${column.capitalColumnName}()));
                    <#else>
                    map.put("${column.exportName}", ${changeClassName}.get${column.capitalColumnName}());
                    </#if>
                    </#if>
                    </#if>
                </#list>
                    list.add(map);
                }
                writer.write(list);
                if (records.size() < pageSize) {
                    break;
                }
                current++;
            }
        });
    }
<#if hasDict>

    private Object getDictLabel(String dictName, Object dictValue) {
        String label = redisUtils.getDictLabel(dictName, dictValue);
        return label == null || label.length() == 0 ? <#if hasLong>getExportValue(dictValue)<#else>dictValue</#if> : label;
    }
</#if>
<#if hasLong>

    private Object getExportValue(Object value) {
        return value instanceof Long ? String.valueOf(value) : value;
    }
</#if>
}
