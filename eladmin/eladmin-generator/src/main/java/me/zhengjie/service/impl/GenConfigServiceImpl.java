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
package me.zhengjie.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.zhengjie.domain.GenConfig;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.mapper.GenConfigMapper;
import me.zhengjie.service.GenConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Zheng Jie
 * @date 2019-01-14
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings({"unchecked","all"})
public class GenConfigServiceImpl extends ServiceImpl<GenConfigMapper, GenConfig> implements GenConfigService {

    private final GenConfigMapper genConfigMapper;

    @Override
    public GenConfig find(String tableName) {
        GenConfig genConfig = genConfigMapper.findByTableName(tableName);
        if(genConfig == null){
            return new GenConfig(tableName);
        }
        return genConfig;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GenConfig update(String tableName, GenConfig genConfig) {
        genConfig.setTableName(tableName);
        genConfig.setApiPath(resolveApiPath(genConfig.getPath()));
        GenConfig existing = genConfigMapper.findByTableName(tableName);
        if (existing != null) {
            genConfig.setId(existing.getId());
            genConfigMapper.updateById(genConfig);
        } else {
            genConfigMapper.insert(genConfig);
        }
        genConfigMapper.deleteDuplicates(tableName, genConfig.getId());
        return genConfig;
    }

    private String resolveApiPath(String viewPathValue) {
        if (StrUtil.isBlank(viewPathValue)) {
            throw new BadRequestException("前端文件路径不能为空");
        }
        Path viewPath = Paths.get(viewPathValue).toAbsolutePath().normalize();
        Path webRoot = findParent(viewPath, "eladmin-web");
        if (webRoot == null) {
            throw new BadRequestException("前端文件路径必须位于 eladmin-web 目录下");
        }
        Path viewsRoot = webRoot.resolve("src").resolve("views").normalize();
        if (!viewPath.startsWith(viewsRoot)) {
            throw new BadRequestException("前端文件路径必须位于 eladmin-web/src/views 下");
        }
        return webRoot.resolve("src").resolve("api").normalize().toString();
    }

    private Path findParent(Path path, String name) {
        Path current = path;
        while (current != null) {
            if (name.equalsIgnoreCase(String.valueOf(current.getFileName()))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }
}
