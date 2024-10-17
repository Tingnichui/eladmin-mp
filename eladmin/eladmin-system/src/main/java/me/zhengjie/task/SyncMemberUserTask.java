package me.zhengjie.task;

import cn.hutool.core.lang.Validator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.gym.domain.JljsMemberInfo;
import me.zhengjie.gym.service.JljsMemberInfoService;
import me.zhengjie.modules.system.domain.Dept;
import me.zhengjie.modules.system.domain.Job;
import me.zhengjie.modules.system.domain.Role;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.modules.system.service.UserService;
import me.zhengjie.utils.RedisUtils;
import me.zhengjie.utils.enums.RedisKeyEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service("syncMemberUserTask")
public class SyncMemberUserTask {

    @Resource
    private RedisUtils redisUtils;
    @Resource
    private JljsMemberInfoService jljsMemberInfoService;
    @Resource
    private UserService userService;
    @Resource
    private PasswordEncoder passwordEncoder;


    public void sync() {
        RedisKeyEnum keyEnum = RedisKeyEnum.SYNC_GYM_MEMBER_USER_TASK;
        boolean lock = redisUtils.setIfAbsent(keyEnum.getKey(), keyEnum.getDesc(), 30, TimeUnit.MINUTES);
        log.info("定时任务：{}，获取锁：{}", keyEnum.getDesc(), lock);
//        if (!lock) return;

        try {
            Role role = new Role();
            role.setId(4L);
            Set<Role> roles = Collections.singleton(role);

            Job job = new Job();
            job.setId(14L);
            Set<Job> jobs = Collections.singleton(job);

            Dept dept = new Dept();
            dept.setId(20L);

            List<JljsMemberInfo> jljsMemberInfoList = jljsMemberInfoService.list();
            for (JljsMemberInfo memberInfo : jljsMemberInfoList) {
                String memberPhoneNum = memberInfo.getMemberPhoneNum();
                if (StringUtils.isBlank(memberPhoneNum) || !Validator.isMobile(memberPhoneNum)) {
                    continue;
                }
                // 如果没有关联到用户id则需要新建一个
                if (null == memberInfo.getUserId()) {
                    User user = new User();
                    user.setUsername(memberPhoneNum);
                    user.setNickName(memberInfo.getMemberName());
                    user.setGender(memberInfo.getMemberGender());
                    user.setEnabled(true);
                    user.setRoles(roles);
                    user.setJobs(jobs);
                    user.setDept(dept);
                    user.setPhone(memberPhoneNum);
                    String rawPassword = "Aa" + memberPhoneNum.substring(memberPhoneNum.length() - 6);
                    user.setPassword(passwordEncoder.encode(rawPassword));
                    userService.create(user);

                    jljsMemberInfoService.update(
                            Wrappers.lambdaUpdate(JljsMemberInfo.class)
                                    .eq(JljsMemberInfo::getId, memberInfo.getId())
                                    .set(JljsMemberInfo::getUserId, user.getId())
                    );
                }
            }

        } catch (Exception e) {
            log.error("{}出现异常", keyEnum.getDesc(), e);
            throw e;
        }
    }
}
