package me.zhengjie.iou.domain.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;
import java.util.List;
import me.zhengjie.iou.service.IouBusinessException;

/** User input only. Identity, totals and signature state never enter this DTO. */
@Data
public class IouNoteConditions {
    private String creatorRole;
    private String principalAmount;
    private String loanDate;
    private String purpose;
    private String remark;
    private String repaymentMethod;
    private Boolean interestFree;
    private String annualRate;
    private String fixedInterestAmount;
    private String dueDate;
    private Integer installmentCount;
    private Integer repaymentCycleMonths;
    private String firstDueDate;
    private List<CustomPeriod> customPlan;
    private String prepaymentRule;
    private String overdueTerms;

    @JsonAnySetter public void rejectUnknown(String key, Object value) {
        throw new IouBusinessException(400, "INVALID_CONDITIONS", "请求包含不允许的字段");
    }
    @Data public static class CustomPeriod {
        private String dueDate;
        private String principalAmount;
        @JsonAnySetter public void rejectUnknown(String key, Object value) {
            throw new IouBusinessException(400, "INVALID_PLAN", "计划只接受日期与本金");
        }
    }
}
