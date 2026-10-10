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
package me.zhengjie.iou.service.impl;

import me.zhengjie.iou.domain.IouUser;
import me.zhengjie.iou.service.IouIdentitySupport;
import me.zhengjie.exception.BadRequestException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import java.sql.Timestamp;
import java.util.Objects;
import me.zhengjie.utils.FileUtil;
import me.zhengjie.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.iou.service.IouUserService;
import me.zhengjie.iou.domain.dto.IouUserQueryCriteria;
import me.zhengjie.iou.mapper.IouUserMapper;
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
* @author chunhui
* @date 2026-10-10
**/
@Service
@RequiredArgsConstructor
public class IouUserServiceImpl extends ServiceImpl<IouUserMapper, IouUser> implements IouUserService {

    private final IouUserMapper iouUserMapper;
    private final RedisUtils redisUtils;
    private final IouIdentitySupport identitySupport;

    @Override
    public PageResult<IouUser> queryAll(IouUserQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(iouUserMapper.findAll(criteria, page));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(IouUser resources) {
        if (resources.getId() != null) {
            throw new BadRequestException("新增用户不能指定ID");
        }
        prepare(resources, true);
        iouUserMapper.insert(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(IouUser resources) {
        IouUser iouUser = getById(resources.getId());
        if (iouUser == null) {
            throw new BadRequestException("用户不存在");
        }
        prepare(resources, false);
        if (!Objects.equals(resources.getMobile(), iouUser.getMobile())
            || (resources.getRealName() != null && !Objects.equals(resources.getRealName(), iouUser.getRealName()))
            || (resources.getIdNoHash() != null && !Objects.equals(resources.getIdNoHash(), iouUser.getIdNoHash()))) {
            resources.setVerifyStatus(0);
            iouUser.setVerifiedAt(null);
        }
        iouUser.copy(resources);
        if (Integer.valueOf(2).equals(iouUser.getVerifyStatus())
            && (!StringUtils.hasText(iouUser.getRealName()) || iouUser.getIdNoHash() == null)) {
            throw new BadRequestException("已认证用户需要完整身份信息");
        }
        iouUser.setVerifiedAt(Integer.valueOf(2).equals(iouUser.getVerifyStatus())
            ? (iouUser.getVerifiedAt() == null ? new Timestamp(System.currentTimeMillis()) : iouUser.getVerifiedAt()) : null);
        iouUser.setUpdateTime(null);
        iouUserMapper.updateById(iouUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        iouUserMapper.deleteBatchIds(ids);
    }

    private void prepare(IouUser user, boolean create) {
        if (user.getMobile() == null || !user.getMobile().matches("1[0-9]{10}")) {
            throw new BadRequestException("请填写11位手机号");
        }
        if (create && !StringUtils.hasText(user.getPassword())) {
            throw new BadRequestException("新增用户需要填写登录密码");
        }
        if (StringUtils.hasText(user.getPassword())) {
            if (user.getPassword().length() < 6 || user.getPassword().length() > 72) {
                throw new BadRequestException("登录密码长度须为6至72个字符");
            }
            user.setPasswordHash(new BCryptPasswordEncoder().encode(user.getPassword()));
        }
        user.setPassword(null);
        if (StringUtils.hasText(user.getIdNo())) {
            user.setIdNoHash(identitySupport.hash(user.getIdNo()));
            user.setIdNoEncrypted(identitySupport.encrypt(user.getIdNo()));
        }
        user.setIdNo(null);
        user.setCreateTime(null);
        user.setUpdateTime(null);
        user.setVerifiedAt(null);
        if (create && user.getVerifyStatus() == null) {
            user.setVerifyStatus(0);
        }
        if (user.getVerifyStatus() != null && (user.getVerifyStatus() < 0 || user.getVerifyStatus() > 3)) {
            throw new BadRequestException("认证状态不合法");
        }
        if (create && Integer.valueOf(2).equals(user.getVerifyStatus())) {
            if (!StringUtils.hasText(user.getRealName()) || user.getIdNoHash() == null) {
                throw new BadRequestException("已认证用户需要完整身份信息");
            }
            user.setVerifiedAt(new Timestamp(System.currentTimeMillis()));
        }
    }

    @Override
    public void download(IouUserQueryCriteria criteria, HttpServletResponse response) throws IOException {
        List<String> headers = new ArrayList<>();
        headers.add("手机号");
        headers.add("真实姓名");
        headers.add("认证状态");
        headers.add("认证通过时间");
        headers.add("创建时间");
        headers.add("更新时间");
        FileUtil.downloadExcel(headers, response, writer -> {
            long current = 1L;
            final long pageSize = 10000L;
            while (true) {
                Page<Object> page = new Page<>(current, pageSize, false);
                List<IouUser> records = iouUserMapper.findAll(criteria, page).getRecords();
                if (records.isEmpty()) {
                    break;
                }
                List<Map<String, Object>> list = new ArrayList<>(records.size());
                for (IouUser iouUser : records) {
                    Map<String,Object> map = new LinkedHashMap<>();
                    map.put("手机号", iouUser.getMobile());
                    map.put("真实姓名", iouUser.getRealName());
                    map.put("认证状态", getDictLabel("iou_user_verify_status", iouUser.getVerifyStatus()));
                    map.put("认证通过时间", iouUser.getVerifiedAt());
                    map.put("创建时间", iouUser.getCreateTime());
                    map.put("更新时间", iouUser.getUpdateTime());
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

    private Object getDictLabel(String dictName, Object dictValue) {
        String label = redisUtils.getDictLabel(dictName, dictValue);
        return label == null || label.length() == 0 ? getExportValue(dictValue) : label;
    }

    private Object getExportValue(Object value) {
        return value instanceof Long ? String.valueOf(value) : value;
    }
}
