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
package me.zhengjie.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.system.domain.Dept;
import me.zhengjie.modules.system.domain.Job;
import me.zhengjie.modules.system.domain.Role;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.modules.system.service.DeptService;
import me.zhengjie.modules.system.service.JobService;
import me.zhengjie.modules.system.service.RoleService;
import me.zhengjie.modules.system.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;

/**
 * 显式启用时创建本地开发账号。密码只从外部配置读取，不在仓库中保存。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "local.dev-user.enabled", havingValue = "true")
public class LocalDevUserInitializer implements ApplicationRunner {

    private final UserService userService;
    private final RoleService roleService;
    private final JobService jobService;
    private final DeptService deptService;
    private final PasswordEncoder passwordEncoder;

    @Value("${local.dev-user.username:chunhui}")
    private String username;

    @Value("${local.dev-user.password:}")
    private String password;

    @Value("${local.dev-user.role-id:1}")
    private Long roleId;

    @Value("${local.dev-user.job-id:11}")
    private Long jobId;

    @Value("${local.dev-user.dept-id:2}")
    private Long deptId;

    @Value("${local.dev-user.email:}")
    private String email;

    @Value("${local.dev-user.phone:19900000000}")
    private String phone;

    @Override
    public void run(ApplicationArguments args) {
        if (userService.findByName(username) != null) {
            log.info("本地开发账号已存在，跳过初始化：{}", username);
            return;
        }
        if (!StringUtils.hasText(password)) {
            throw new IllegalStateException("启用本地开发账号初始化时必须配置 LOCAL_DEV_USER_PASSWORD");
        }

        Role role = requireReference(roleService.getById(roleId), "角色", roleId);
        Job job = requireReference(jobService.getById(jobId), "岗位", jobId);
        Dept dept = requireReference(deptService.getById(deptId), "部门", deptId);

        User user = new User();
        user.setUsername(username);
        user.setNickName(username);
        user.setGender("男");
        user.setEmail(StringUtils.hasText(email) ? email : username + "@local.invalid");
        user.setPhone(phone);
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(password));
        user.setPwdResetTime(new Date());
        user.setDept(dept);
        user.setRoles(Collections.singleton(role));
        user.setJobs(Collections.singleton(job));
        userService.create(user);
        log.info("已创建本地开发账号：{}", username);
    }

    private <T> T requireReference(T value, String type, Long id) {
        if (value == null) {
            throw new IllegalStateException("本地开发账号初始化失败，" + type + "不存在，ID：" + id);
        }
        return value;
    }
}
