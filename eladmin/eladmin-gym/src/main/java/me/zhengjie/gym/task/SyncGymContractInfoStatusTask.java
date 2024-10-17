package me.zhengjie.gym.task;


import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.gym.constants.*;
import me.zhengjie.gym.domain.GymClassRecord;
import me.zhengjie.gym.domain.GymContractInfo;
import me.zhengjie.gym.domain.GymContractOperateRecord;
import me.zhengjie.gym.mapper.GymClassRecordMapper;
import me.zhengjie.gym.mapper.GymContractInfoMapper;
import me.zhengjie.gym.mapper.GymContractOperateRecordMapper;
import me.zhengjie.utils.RedisUtils;
import me.zhengjie.utils.enums.RedisKeyEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service("syncGymContractInfoStatusTask")
public class SyncGymContractInfoStatusTask {

    @Resource
    private GymContractInfoMapper gymContractInfoMapper;
    @Resource
    private GymContractOperateRecordMapper gymContractOperateRecordMapper;
    @Resource
    private GymClassRecordMapper gymClassRecordMapper;
    @Resource
    private RedisUtils redisUtils;

    public void syncAll() {
        try {
            RedisKeyEnum keyEnum = RedisKeyEnum.SYNC_ALL_CONTRACT_INFO_TASK;
            boolean lock = redisUtils.setIfAbsent(keyEnum.getKey(), keyEnum.getDesc(), 30, TimeUnit.MINUTES);
            log.info("定时任务：{}，获取锁：{}", keyEnum.getDesc(), lock);
            if (!lock) return;

            List<GymContractInfo> gymContractInfoList = gymContractInfoMapper.selectList(Wrappers.lambdaQuery(GymContractInfo.class).select(GymContractInfo::getId));
            for (GymContractInfo contractInfo : gymContractInfoList) {
                this.sync(contractInfo.getId());
            }
        } catch (Exception e) {
            log.error("批量同步合同信息出现异常", e);
            throw e;
        }
    }

    public void sync(String contractId) {
        Date today = DateUtil.beginOfDay(new Date());

        GymContractInfo contractInfo = gymContractInfoMapper.selectById(contractId);
        if (null == contractInfo) {
            log.info("合同不存在，合同id：{}", contractId);
            return;
        }

        // 获取开卡记录 1.没有开卡记录设置为待开卡，重置开始时间、结束时间、总暂停天数 2.有开卡记录设置使用时间
        GymContractOperateRecord kaikaRecord = gymContractOperateRecordMapper.selectOne(
                getBaseSelect(contractId).eq(GymContractOperateRecord::getContractOperateType, GymContractOperateTypeEnum.kaika.getCode())
        );
        if (null == kaikaRecord) {
            log.info("合同没有开卡记录，合同id：{}", contractId);
            gymContractInfoMapper.update(null,
                    Wrappers.lambdaUpdate(GymContractInfo.class)
                            .eq(GymContractInfo::getId, contractId)
                            .set(GymContractInfo::getUseBeginDate, null)
                            .set(GymContractInfo::getUseEndDate, null)
                            .set(GymContractInfo::getCourseUseQuantity, null)
                            .set(GymContractInfo::getCourseRemainQuantity, null)
                            .set(GymContractInfo::getCourseTotalStopDays, null)
                            .set(GymContractInfo::getContractStatus, GymContractStatusEnum.daikaika.getCode())
            );
            return;
        }
        contractInfo.setUseBeginDate(kaikaRecord.getOperateBeginDate());
        contractInfo.setContractStatus(GymContractStatusEnum.shiyong.getCode());

        // 获取请假记录 计算总暂停天数 重新计算有效期使用天厨
        List<GymContractOperateRecord> zantingList = gymContractOperateRecordMapper.selectList(
                getBaseSelect(contractId).eq(GymContractOperateRecord::getContractOperateType, GymContractOperateTypeEnum.zanting.getCode())
        );
        int totalStopDays = zantingList.stream().mapToInt(GymContractOperateRecord::getIntervalDays).sum();
        contractInfo.setCourseTotalStopDays(totalStopDays);
        // 设置结束时间为开始时间之后的 总有效期+暂停天数
        DateTime dateTime = DateUtil.parseDateTime(DateUtil.format(DateUtil.offsetDay(contractInfo.getUseBeginDate(), totalStopDays + contractInfo.getCourseUsePeriodDays() - 1), "yyyy-MM-dd 23:59:59"));
        contractInfo.setUseEndDate(dateTime.toTimestamp());
        // 暂停操作记录不为空，遍历判断是否在暂停中
        if (!zantingList.isEmpty()) {
            // 默认设置为使用中
            contractInfo.setContractStatus(GymContractStatusEnum.shiyong.getCode());
            // 如果在请假时间内 则设置为暂停
            for (GymContractOperateRecord record : zantingList) {
                if (DateUtil.isIn(today, record.getOperateBeginDate(), record.getOperateEndDate())) {
                    contractInfo.setContractStatus(GymContractStatusEnum.zanting.getCode());
                    break;
                }
            }
        }

        // 获取该会员该合同的使用记录
        int realUseCount = Math.toIntExact(gymClassRecordMapper.selectCount(
                Wrappers.lambdaQuery(GymClassRecord.class)
                        .eq(GymClassRecord::getMemberId, contractInfo.getMemberId())
                        .eq(GymClassRecord::getContractInfoId, contractId)
        ));
        contractInfo.setCourseUseQuantity(realUseCount);

        // 更新 次卡已使用量、剩余数量
        String courseType = contractInfo.getCourseType();
        if (GymCourseTypeEnum.ci.getCode().equals(courseType)) {
            contractInfo.setCourseRemainQuantity(contractInfo.getCourseAvailableQuantity() - realUseCount);
        }
        // 更新 按天计时 已使用量、剩余数量
        if (GymCourseTypeEnum.tian.getCode().equals(courseType)) {
            // 已使用量 = 使用开始时间到今天的天数 - 总计的暂停天数
            int courseUsePeriodDays = (int) DateUtil.betweenDay(contractInfo.getUseBeginDate(), today, true) - totalStopDays;
//            contractInfo.setCourseUseQuantity(Math.min(contractInfo.getCourseAvailableQuantity(), courseUsePeriodDays));
            contractInfo.setCourseRemainQuantity(Math.max(contractInfo.getCourseAvailableQuantity() - courseUsePeriodDays, 0));
        }


        // 获取补缴记录 计算实际收取金额
        List<GymContractOperateRecord> bujiaoList = gymContractOperateRecordMapper.selectList(
                getBaseSelect(contractId).eq(GymContractOperateRecord::getContractOperateType, GymContractOperateTypeEnum.bujiao.getCode())
        );
        if (CollectionUtils.isNotEmpty(bujiaoList)) {

        }

        // 使用期限已经过了
        if (contractInfo.getUseEndDate().compareTo(today) < 0) {
            // 如果是使用中 更新为已完成
            if (GymContractStatusEnum.shiyong.getCode().equals(contractInfo.getContractStatus())) {
                contractInfo.setContractStatus(GymContractStatusEnum.wancheng.getCode());
            }
        }
        // 如果是次卡 剩余数量为0
        if (GymCourseTypeEnum.ci.getCode().equals(courseType) && Objects.equals(contractInfo.getCourseRemainQuantity(), 0)) {
            // 如果是使用中 更新为已完成
            if (GymContractStatusEnum.shiyong.getCode().equals(contractInfo.getContractStatus())) {
                contractInfo.setContractStatus(GymContractStatusEnum.wancheng.getCode());
            }
        }


        // 合同是否终止
        GymContractOperateRecord tuikeRecord = gymContractOperateRecordMapper.selectOne(
                getBaseSelect(contractId).eq(GymContractOperateRecord::getContractOperateType, GymContractOperateTypeEnum.tuike.getCode())
        );
        if (null != tuikeRecord) {
            contractInfo.setUseBeginDate(tuikeRecord.getOperateBeginDate());
            contractInfo.setContractStatus(GymContractStatusEnum.zhongzhi.getCode());
        }

        // 更新合同信息
        gymContractInfoMapper.updateById(contractInfo);

    }

    private LambdaQueryWrapper<GymContractOperateRecord> getBaseSelect(String contractId) {
        LambdaQueryWrapper<GymContractOperateRecord> queryWrapper = Wrappers.lambdaQuery(GymContractOperateRecord.class);
        queryWrapper.eq(GymContractOperateRecord::getContractInfoId, contractId);
        queryWrapper.eq(GymContractOperateRecord::getOperateStatus, GymContractOperateStatusEnum.chenggong.getCode());
        return queryWrapper;
    }


}
