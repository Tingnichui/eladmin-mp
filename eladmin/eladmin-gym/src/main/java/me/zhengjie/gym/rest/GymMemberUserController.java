package me.zhengjie.gym.rest;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.annotation.Log;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.gym.domain.GymClassRecord;
import me.zhengjie.gym.domain.GymContractInfo;
import me.zhengjie.gym.domain.GymContractOperateRecord;
import me.zhengjie.gym.domain.GymMemberInfo;
import me.zhengjie.gym.domain.vo.GymClassRecordQueryCriteria;
import me.zhengjie.gym.domain.vo.GymContractInfoQueryCriteria;
import me.zhengjie.gym.domain.vo.GymContractOperateRecordQueryCriteria;
import me.zhengjie.gym.domain.vo.GymUserHomeFuncConfig;
import me.zhengjie.gym.service.GymClassRecordService;
import me.zhengjie.gym.service.GymContractInfoService;
import me.zhengjie.gym.service.GymContractOperateRecordService;
import me.zhengjie.gym.service.GymMemberInfoService;
import me.zhengjie.utils.PageResult;
import me.zhengjie.utils.SecurityUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Api(tags = "健身房会员用户H5")
@RequestMapping("/api/h5/")
public class GymMemberUserH5Controller {

    @Resource
    private GymMemberInfoService gymMemberInfoService;
    @Resource
    private GymContractInfoService gymContractInfoService;
    @Resource
    private GymClassRecordService gymClassRecordService;
    @Resource
    private GymContractOperateRecordService gymContractOperateRecordService;

    @GetMapping("getGymUserHomeFuncConfig")
    @Log("查询首页功能配置")
    @ApiOperation("查询上课记录")
    @PreAuthorize("@el.check('gymMember:homeFuncConf:list')")
    public ResponseEntity<List<GymUserHomeFuncConfig>> getGymUserHomeFuncConfig() {
        GymMemberInfo memberInfo = this.getJljsMemberInfo();
        String memberInfoId = memberInfo.getId();


        String configStr = "[\n" +
                "  {\n" +
                "    icon: 'contract',\n" +
                "    text: '我的合同',\n" +
                "    color: '53b7ad',\n" +
                "    count: '',\n" +
                "    jumpUrl: '/gym/contract',\n" +
                "    countSql: 'SELECT COUNT(1) FROM jljs_contract_info WHERE del_flag = 0 and member_id = {member_id}',\n" +
                "  },\n" +
                "  {\n" +
                "    icon: 'gym',\n" +
                "    text: '上课记录',\n" +
                "    color: '6ecc84',\n" +
                "    count: '',\n" +
                "    jumpUrl: '/gym/classRecord',\n" +
                "    countSql: 'SELECT COUNT(1) FROM jljs_class_record WHERE del_flag = 0 and member_id = {member_id}',\n" +
                "  },\n" +
                "  {\n" +
                "    icon: 'rest',\n" +
                "    text: '请假记录',\n" +
                "    color: '4580dd',\n" +
                "    count: '',\n" +
                "    jumpUrl: '/gym/contractOperateRecord',\n" +
                "    countSql: 'SELECT COUNT(1) FROM jljs_contract_operate_record WHERE del_flag = 0 and contract_info_id IN (SELECT id FROM jljs_contract_info WHERE del_flag = 0 and member_id = {member_id})',\n" +
                "  },\n" +
                "]";

        List<GymUserHomeFuncConfig> gymUserHomeFuncConfigs = JSON.parseArray(configStr, GymUserHomeFuncConfig.class);
        for (GymUserHomeFuncConfig gymUserHomeFuncConfig : gymUserHomeFuncConfigs) {
            String countSql = gymUserHomeFuncConfig.getCountSql();
            if (StringUtils.isBlank(countSql)) {
                continue;
            }
            countSql = countSql.replaceAll("\\{member_id}", memberInfoId);
            try {
                long count = SqlRunner.db().selectCount(countSql);
                gymUserHomeFuncConfig.setCount(String.valueOf(count));
            } catch (Exception e) {
                log.error("查询计数出现异常", e);
            }
        }


        return new ResponseEntity<>(gymUserHomeFuncConfigs, HttpStatus.OK);
    }

    @GetMapping("/getGymClassRecordList")
    @Log("查询上课记录")
    @ApiOperation("查询上课记录")
    @PreAuthorize("@el.check('gymMember:classRecord:list')")
    public ResponseEntity<PageResult<GymClassRecord>> getGymClassRecordList(GymClassRecordQueryCriteria criteria, Page<Object> page){
        GymMemberInfo memberInfo = this.getJljsMemberInfo();
        List<GymContractInfo> contractInfoList = gymContractInfoService.list(
                Wrappers.lambdaQuery(GymContractInfo.class)
                        .eq(GymContractInfo::getMemberId, memberInfo.getId())
        );
        if (CollectionUtils.isEmpty(contractInfoList)) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        criteria.setContractInfoIdList(contractInfoList.stream().map(GymContractInfo::getId).collect(Collectors.toList()));
        return new ResponseEntity<>(gymClassRecordService.queryAll(criteria,page),HttpStatus.OK);
    }

    @GetMapping("/getGymContractOperateRecordList")
    @Log("会员:查询合同操作记录")
    @ApiOperation("会员:查询合同操作记录")
    @PreAuthorize("@el.check('gymMember:contractOperateRecord:list')")
    public ResponseEntity<PageResult<GymContractOperateRecord>> getGymContractOperateRecordList(GymContractOperateRecordQueryCriteria criteria, Page<Object> page){
        GymMemberInfo memberInfo = this.getJljsMemberInfo();
        List<GymContractInfo> contractInfoList = gymContractInfoService.list(
                Wrappers.lambdaQuery(GymContractInfo.class)
                        .eq(GymContractInfo::getMemberId, memberInfo.getId())
        );
        if (CollectionUtils.isEmpty(contractInfoList)) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        criteria.setContractInfoIdList(contractInfoList.stream().map(GymContractInfo::getId).collect(Collectors.toList()));
        return new ResponseEntity<>(gymContractOperateRecordService.queryAll(criteria,page),HttpStatus.OK);
    }

    @GetMapping("/getGymContractInfoList")
    @Log("会员:查询合同管理")
    @ApiOperation("会员:查询合同管理")
    @PreAuthorize("@el.check('gymMember:contractInfo:list')")
    public ResponseEntity<PageResult<GymContractInfo>> getGymContractInfoList(GymContractInfoQueryCriteria criteria, Page<Object> page){
        GymMemberInfo memberInfo = this.getJljsMemberInfo();
        criteria.setMemberId(memberInfo.getId());
        return new ResponseEntity<>(gymContractInfoService.queryAll(criteria,page),HttpStatus.OK);
    }

    private GymMemberInfo getJljsMemberInfo() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        GymMemberInfo memberInfo = gymMemberInfoService.getBaseMapper().selectOne(
                Wrappers.lambdaQuery(GymMemberInfo.class)
                        .eq(GymMemberInfo::getUserId, currentUserId)
        );
        if (null == memberInfo) {
            throw new BadRequestException("当前用户不是健身会员");
        }
        return memberInfo;
    }

}
