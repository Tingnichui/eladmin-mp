package me.zhengjie.jljs.rest;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.annotation.Log;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.jljs.domain.JljsMemberInfo;
import me.zhengjie.jljs.domain.vo.UserHomeFuncConfig;
import me.zhengjie.jljs.service.JljsMemberInfoService;
import me.zhengjie.utils.SecurityUtils;
import me.zhengjie.utils.SpringContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Api(tags = "健身房会员用户H5")
@RequestMapping("/api/h5/")
public class GymMemberUserController {

    private final JljsMemberInfoService jljsMemberInfoService;

    @GetMapping
    @Log("查询首页功能配置")
    @ApiOperation("查询上课记录")
    @PreAuthorize("@el.check('gymMember:homeFuncConf:list')")
    public ResponseEntity<List<UserHomeFuncConfig>> queryJljsClassRecord() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        JljsMemberInfo memberInfo = jljsMemberInfoService.getBaseMapper().selectOne(
                Wrappers.lambdaQuery(JljsMemberInfo.class)
                        .eq(JljsMemberInfo::getUserId, currentUserId)
        );
        if (null == memberInfo) {
            throw new BadRequestException("当前用户不是健身会员");
        }
        String memberInfoId = memberInfo.getId();


        String configStr = "[\n" +
                "  {\n" +
                "    icon: 'contract',\n" +
                "    text: '我的合同',\n" +
                "    color: '53b7ad',\n" +
                "    count: '',\n" +
                "    jumpUrl: '/gym/contract',\n" +
                "    countSql: 'SELECT COUNT(1) FROM jljs_contract_info WHERE member_id = {member_id}',\n" +
                "  },\n" +
                "  {\n" +
                "    icon: 'gym',\n" +
                "    text: '上课记录',\n" +
                "    color: '6ecc84',\n" +
                "    count: '',\n" +
                "    jumpUrl: '/gym/classRecord',\n" +
                "    countSql: 'SELECT COUNT(1) FROM jljs_class_record WHERE member_id = {member_id}',\n" +
                "  },\n" +
                "  {\n" +
                "    icon: 'rest',\n" +
                "    text: '请假记录',\n" +
                "    color: '4580dd',\n" +
                "    count: '',\n" +
                "    jumpUrl: '/gym/contractOperateRecord',\n" +
                "    countSql: 'SELECT COUNT(1) FROM jljs_contract_operate_record WHERE contract_info_id IN (SELECT id FROM jljs_contract_info WHERE member_id = {member_id})',\n" +
                "  },\n" +
                "]";

        List<UserHomeFuncConfig> userHomeFuncConfigs = JSON.parseArray(configStr, UserHomeFuncConfig.class);
        for (UserHomeFuncConfig userHomeFuncConfig : userHomeFuncConfigs) {
            String countSql = userHomeFuncConfig.getCountSql();
            if (StringUtils.isBlank(countSql)) {
                continue;
            }
            countSql = countSql.replaceAll("\\{member_id}", memberInfoId);
            try {
                long count = SqlRunner.db().selectCount(countSql);
                userHomeFuncConfig.setCount(String.valueOf(count));
            } catch (Exception e) {
                log.error("查询计数出现异常", e);
            }
        }


        return new ResponseEntity<>(userHomeFuncConfigs, HttpStatus.OK);
    }

}
