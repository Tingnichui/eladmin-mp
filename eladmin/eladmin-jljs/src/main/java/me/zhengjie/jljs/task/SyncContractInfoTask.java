package me.zhengjie.jljs.task;


import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.jljs.constants.*;
import me.zhengjie.jljs.domain.JljsClassRecord;
import me.zhengjie.jljs.domain.JljsContractInfo;
import me.zhengjie.jljs.domain.JljsContractOperateRecord;
import me.zhengjie.jljs.mapper.JljsClassRecordMapper;
import me.zhengjie.jljs.mapper.JljsContractInfoMapper;
import me.zhengjie.jljs.mapper.JljsContractOperateRecordMapper;
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
@Service("syncContractInfoTask")
public class SyncContractInfoTask {

    @Resource
    private JljsContractInfoMapper jljsContractInfoMapper;
    @Resource
    private JljsContractOperateRecordMapper jljsContractOperateRecordMapper;
    @Resource
    private JljsClassRecordMapper jljsClassRecordMapper;
    @Resource
    private RedisUtils redisUtils;

    public void syncAll() {
        try {
            RedisKeyEnum keyEnum = RedisKeyEnum.SYNC_ALL_CONTRACT_INFO_TASK;
            boolean lock = redisUtils.setIfAbsent(keyEnum.getKey(), keyEnum.getKey(), 30, TimeUnit.MINUTES);
            log.info("定时任务：{}，获取锁：{}", keyEnum.getDesc(), lock);
            if (!lock) return;

            List<JljsContractInfo> jljsContractInfoList = jljsContractInfoMapper.selectList(Wrappers.lambdaQuery(JljsContractInfo.class).select(JljsContractInfo::getId));
            for (JljsContractInfo contractInfo : jljsContractInfoList) {
                this.sync(contractInfo.getId());
            }
        } catch (Exception e) {
            log.error("批量同步合同信息出现异常", e);
            throw e;
        }
    }

    public void sync(String contractId) {
        Date today = DateUtil.beginOfDay(new Date());

        JljsContractInfo contractInfo = jljsContractInfoMapper.selectById(contractId);
        if (null == contractInfo) {
            log.info("合同不存在，合同id：{}", contractId);
            return;
        }

        // 获取开卡记录 1.没有开卡记录设置为待开卡，重置开始时间、结束时间、总暂停天数 2.有开卡记录设置使用时间
        JljsContractOperateRecord kaikaRecord = jljsContractOperateRecordMapper.selectOne(
                getBaseSelect(contractId).eq(JljsContractOperateRecord::getContractOperateType, JljsContractOperateTypeEnum.kaika.getCode())
        );
        if (null == kaikaRecord) {
            log.info("合同没有开卡记录，合同id：{}", contractId);
            jljsContractInfoMapper.update(null,
                    Wrappers.lambdaUpdate(JljsContractInfo.class)
                            .eq(JljsContractInfo::getId, contractId)
                            .set(JljsContractInfo::getUseBeginDate, null)
                            .set(JljsContractInfo::getUseEndDate, null)
                            .set(JljsContractInfo::getCourseUseQuantity, null)
                            .set(JljsContractInfo::getCourseRemainQuantity, null)
                            .set(JljsContractInfo::getCourseTotalStopDays, null)
                            .set(JljsContractInfo::getContractStatus, JljsContractStatusEnum.daikaika.getCode())
            );
            return;
        }
        contractInfo.setUseBeginDate(kaikaRecord.getOperateBeginDate());
        contractInfo.setContractStatus(JljsContractStatusEnum.shiyong.getCode());

        // 获取请假记录 计算总暂停天数 重新计算有效期使用天厨
        List<JljsContractOperateRecord> zantingList = jljsContractOperateRecordMapper.selectList(
                getBaseSelect(contractId).eq(JljsContractOperateRecord::getContractOperateType, JljsContractOperateTypeEnum.zanting.getCode())
        );
        int totalStopDays = zantingList.stream().mapToInt(JljsContractOperateRecord::getIntervalDays).sum();
        contractInfo.setCourseTotalStopDays(totalStopDays);
        // 设置结束时间为开始时间之后的 总有效期+暂停天数
        DateTime dateTime = DateUtil.parseDateTime(DateUtil.format(DateUtil.offsetDay(contractInfo.getUseBeginDate(), totalStopDays + contractInfo.getCourseUsePeriodDays() - 1), "yyyy-MM-dd 23:59:59"));
        contractInfo.setUseEndDate(dateTime.toTimestamp());
        // 暂停操作记录不为空，遍历判断是否在暂停中
        if (!zantingList.isEmpty()) {
            // 默认设置为使用中
            contractInfo.setContractStatus(JljsContractStatusEnum.shiyong.getCode());
            // 如果在请假时间内 则设置为暂停
            for (JljsContractOperateRecord record : zantingList) {
                if (DateUtil.isIn(today, record.getOperateBeginDate(), record.getOperateEndDate())) {
                    contractInfo.setContractStatus(JljsContractStatusEnum.zanting.getCode());
                    break;
                }
            }
        }

        // 更新 次卡已使用量、剩余数量
        String courseType = contractInfo.getCourseType();
        if (JljsCourseTypeEnum.ci.getCode().equals(courseType)) {
            // 获取该会员该合同的使用记录
            int count = Math.toIntExact(jljsClassRecordMapper.selectCount(
                    Wrappers.lambdaQuery(JljsClassRecord.class)
                            .eq(JljsClassRecord::getMemberId, contractInfo.getMemberId())
                            .eq(JljsClassRecord::getContractInfoId, contractId)
            ));
            contractInfo.setCourseUseQuantity(count);
            contractInfo.setCourseRemainQuantity(contractInfo.getCourseAvailableQuantity() - count);
        }
        // 更新 按天计时 已使用量、剩余数量
        if (JljsCourseTypeEnum.tian.getCode().equals(courseType)) {
            // 已使用量 = 使用开始时间到今天的天数 - 总计的暂停天数
            int courseUsePeriodDays = (int) DateUtil.betweenDay(contractInfo.getUseBeginDate(), today, true) - totalStopDays;
            contractInfo.setCourseUseQuantity(Math.min(contractInfo.getCourseAvailableQuantity(), courseUsePeriodDays));
            contractInfo.setCourseRemainQuantity(Math.max(contractInfo.getCourseAvailableQuantity() - courseUsePeriodDays, 0));
        }


        // 获取补缴记录 计算实际收取金额
        List<JljsContractOperateRecord> bujiaoList = jljsContractOperateRecordMapper.selectList(
                getBaseSelect(contractId).eq(JljsContractOperateRecord::getContractOperateType, JljsContractOperateTypeEnum.bujiao.getCode())
        );
        if (CollectionUtils.isNotEmpty(bujiaoList)) {

        }

        // 使用期限已经过了
        if (contractInfo.getUseEndDate().compareTo(today) < 0) {
            // 如果是使用中 更新为已完成
            if (JljsContractStatusEnum.shiyong.getCode().equals(contractInfo.getContractStatus())) {
                contractInfo.setContractStatus(JljsContractStatusEnum.wancheng.getCode());
            }
        }
        // 如果是次卡 剩余数量为0
        if (JljsCourseTypeEnum.ci.getCode().equals(courseType) && Objects.equals(contractInfo.getCourseRemainQuantity(), 0)) {
            // 如果是使用中 更新为已完成
            if (JljsContractStatusEnum.shiyong.getCode().equals(contractInfo.getContractStatus())) {
                contractInfo.setContractStatus(JljsContractStatusEnum.wancheng.getCode());
            }
        }


        // 合同是否终止
        JljsContractOperateRecord tuikeRecord = jljsContractOperateRecordMapper.selectOne(
                getBaseSelect(contractId).eq(JljsContractOperateRecord::getContractOperateType, JljsContractOperateTypeEnum.tuike.getCode())
        );
        if (null != tuikeRecord) {
            contractInfo.setUseBeginDate(tuikeRecord.getOperateBeginDate());
            contractInfo.setContractStatus(JljsContractStatusEnum.zhongzhi.getCode());
        }

        // 更新合同信息
        jljsContractInfoMapper.updateById(contractInfo);

    }

    private LambdaQueryWrapper<JljsContractOperateRecord> getBaseSelect(String contractId) {
        LambdaQueryWrapper<JljsContractOperateRecord> queryWrapper = Wrappers.lambdaQuery(JljsContractOperateRecord.class);
        queryWrapper.eq(JljsContractOperateRecord::getContractInfoId, contractId);
        queryWrapper.eq(JljsContractOperateRecord::getOperateStatus, JljsOperateStatusEnum.chenggong.getCode());
        return queryWrapper;
    }


}
