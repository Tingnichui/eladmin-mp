package me.zhengjie.iou.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.zhengjie.iou.domain.IouNotePlan;
import me.zhengjie.iou.domain.dto.IouNoteConditions;
import org.springframework.stereotype.Service;
import java.math.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IouRepaymentCalculator {
    public static final String VERSION = "decimal-v1";
    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_UP);
    private final IouNoteOptions options;
    @Data public static class Quote {
        private IouNoteConditions conditions;
        private List<IouNotePlan> plan;
        private String interestRule = "mixed-365-month-v1";
        private String calculationVersion = VERSION;
        private Map<String, String> summary;
        private List<String> warnings = Collections.singletonList("还款计划余额为约定余额，不代表实际还款余额");
    }
    public Quote calculate(IouNoteConditions input) {
        if (input == null) invalid("请填写借款条件");
        // Fresh normalized copy; quote/create/edit have exactly the same calculation.
        IouNoteConditions c = new IouNoteConditions();
        c.setCreatorRole(input.getCreatorRole()); c.setPurpose(text(input.getPurpose(),255));
        c.setRemark(text(input.getRemark(),500)); c.setOverdueTerms(text(input.getOverdueTerms(),1000));
        if (!Arrays.asList("actual", "agreement").contains(input.getPrepaymentRule())) invalid("请选择提前还款约定");
        c.setPrepaymentRule(input.getPrepaymentRule());
        BigDecimal p = money(input.getPrincipalAmount());
        if (p.signum() <= 0 || p.compareTo(options.getPrincipalMax()) > 0) invalid("本金超出可受理范围");
        c.setPrincipalAmount(p.toPlainString());
        LocalDate start = date(input.getLoanDate()); c.setLoanDate(start.toString());
        String method = input.getRepaymentMethod();
        if (!Arrays.asList("once","annuity","principal","interest","flat","custom").contains(method)) invalid("还款方式不正确");
        if (input.getInterestFree() == null) invalid("请选择是否无息");
        boolean free = input.getInterestFree();
        BigDecimal rate = rate(input.getAnnualRate());
        BigDecimal fixed = input.getFixedInterestAmount() == null ? BigDecimal.ZERO.setScale(2) : money(input.getFixedInterestAmount());
        if (free && (rate.signum()!=0 || fixed.signum()!=0)) invalid("无息借款不能同时约定利息");
        if (!"flat".equals(method) && fixed.signum()!=0) invalid("当前方式不接受固定总利息");
        if ("flat".equals(method) && rate.signum()!=0) invalid("固定本息请使用约定总利息");
        if ("flat".equals(method) && !free && input.getFixedInterestAmount()==null) invalid("请填写约定总利息");
        // Zero-rate form inputs normalize identically on save and reload.
        if(rate.signum()==0 && fixed.signum()==0) free=true;
        c.setInterestFree(free);
        if (free && Arrays.asList("annuity","principal","flat").contains(method)) method = "principal";
        c.setAnnualRate(rate.setScale(8).toPlainString()); c.setFixedInterestAmount("flat".equals(method)?fixed.toPlainString():null);
        c.setRepaymentMethod(method);
        List<LocalDate> dates = new ArrayList<>();
        List<BigDecimal> custom = new ArrayList<>();
        int cycle = input.getRepaymentCycleMonths()==null?0:input.getRepaymentCycleMonths();
        boolean regularFirst = false;
        if ("once".equals(method)) {
            if (input.getCustomPlan()!=null || input.getInstallmentCount()!=null || input.getFirstDueDate()!=null || input.getRepaymentCycleMonths()!=null) invalid("一次性还款只需到期日期");
            dates.add(date(input.getDueDate())); c.setDueDate(dates.get(0).toString());
        } else if ("custom".equals(method)) {
            if (input.getDueDate()!=null || input.getInstallmentCount()!=null || input.getFirstDueDate()!=null || input.getRepaymentCycleMonths()!=null) invalid("自定义计划请直接约定各期日期");
            if (input.getCustomPlan()==null || input.getCustomPlan().size()<2 || input.getCustomPlan().size()>60) invalid("自定义计划须为2至60期");
            BigDecimal sum=BigDecimal.ZERO; List<IouNoteConditions.CustomPeriod> normalized = new ArrayList<>();
            for (IouNoteConditions.CustomPeriod period:input.getCustomPlan()) {
                if(period==null) invalid("还款计划不完整");
                LocalDate d=date(period.getDueDate()); BigDecimal amount=money(period.getPrincipalAmount());
                dates.add(d); custom.add(amount); sum=sum.add(amount);
                IouNoteConditions.CustomPeriod n=new IouNoteConditions.CustomPeriod();n.setDueDate(d.toString());n.setPrincipalAmount(amount.toPlainString());normalized.add(n);
            }
            if(sum.compareTo(p)!=0) invalid("各期本金合计必须等于借款本金");
            c.setCustomPlan(normalized);
        } else {
            if(input.getDueDate()!=null || input.getCustomPlan()!=null) invalid("标准分期不接受手工到期日期或自定义计划");
            Integer count=input.getInstallmentCount();
            if(count==null || count<2 || count>60 || !Arrays.asList(1,3).contains(cycle)) invalid("期数须为2至60，周期为1或3个月");
            LocalDate first=date(input.getFirstDueDate()); regularFirst=first.equals(start.plusMonths(cycle));
            for(int i=0;i<count;i++) dates.add(regularFirst?start.plusMonths((long)(i+1)*cycle):first.plusMonths((long)i*cycle));
            c.setInstallmentCount(count);c.setRepaymentCycleMonths(cycle);c.setFirstDueDate(first.toString());
        }
        LocalDate previous=start; List<BigDecimal> periodRates=new ArrayList<>();
        for(int i=0;i<dates.size();i++) {
            LocalDate d=dates.get(i); long days=ChronoUnit.DAYS.between(previous,d);
            if(days<=0 || ChronoUnit.DAYS.between(start,d)>options.getMaxTermDays()) invalid("还款日期须递增且在可受理期限内");
            boolean monthly=Arrays.asList("annuity","principal").contains(method)&&(i>0||regularFirst);
            periodRates.add(rate.multiply(BigDecimal.valueOf(monthly?cycle:days),MC).divide(BigDecimal.valueOf(monthly?12:365),MC));
            previous=d;
        }
        BigDecimal factor=BigDecimal.ONE, discounts=BigDecimal.ZERO;
        for(BigDecimal r:periodRates){factor=factor.divide(BigDecimal.ONE.add(r),MC);discounts=discounts.add(factor,MC);}
        BigDecimal payment=p.divide(discounts,MC).setScale(2,RoundingMode.HALF_UP);
        BigDecimal part=p.divide(BigDecimal.valueOf(dates.size()),2,RoundingMode.DOWN);
        BigDecimal interestPart=fixed.divide(BigDecimal.valueOf(dates.size()),2,RoundingMode.HALF_UP);
        BigDecimal remaining=p, interestTotal=BigDecimal.ZERO.setScale(2), allocated=BigDecimal.ZERO.setScale(2),max=BigDecimal.ZERO;
        List<IouNotePlan> plan=new ArrayList<>();
        for(int i=0;i<dates.size();i++) {
            boolean last=i==dates.size()-1;
            BigDecimal interest=remaining.multiply(periodRates.get(i),MC).setScale(2,RoundingMode.HALF_UP);
            if("flat".equals(method)){interest=last?fixed.subtract(allocated):interestPart.min(fixed.subtract(allocated));allocated=allocated.add(interest);}
            BigDecimal principal;
            if("custom".equals(method)) principal=custom.get(i);
            else if(last) principal=remaining;
            else if("interest".equals(method)) principal=BigDecimal.ZERO.setScale(2);
            else if("annuity".equals(method)) principal=payment.subtract(interest).min(remaining);
            else principal=part;
            if(principal.signum()<0 || principal.compareTo(remaining)>0) invalid("首期日期与利率不能生成合理计划，请调整安排");
            remaining=remaining.subtract(principal);interestTotal=interestTotal.add(interest);
            IouNotePlan row=new IouNotePlan();row.setPeriodNo(i+1);row.setDueDate(dates.get(i));row.setPrincipalAmount(principal);
            row.setInterestAmount(interest);row.setTotalAmount(principal.add(interest));row.setRemainingPrincipal(remaining);plan.add(row);max=max.max(row.getTotalAmount());
        }
        if(remaining.signum()!=0) invalid("计划本金未完全分配");
        Map<String,String>s=new LinkedHashMap<>();s.put("principalAmount",p.toPlainString());s.put("interestAmount",interestTotal.toPlainString());
        s.put("totalAmount",p.add(interestTotal).toPlainString());s.put("dueDate",dates.get(dates.size()-1).toString());
        s.put("firstAmount",plan.get(0).getTotalAmount().toPlainString());s.put("lastAmount",plan.get(plan.size()-1).getTotalAmount().toPlainString());s.put("maximumAmount",max.toPlainString());
        Quote q=new Quote();q.setConditions(c);q.setPlan(plan);q.setSummary(s);return q;
    }
    private BigDecimal rate(String value) {
        if(value==null) return BigDecimal.ZERO;
        if(!value.matches("[0-9]{1,3}(\\.[0-9]{1,8})?")) {invalid("年利率须为比例值，最多八位小数");}
        BigDecimal r=new BigDecimal(value);if(r.compareTo(options.getRateMax())>0)invalid("利率超出当前受理范围");return r;
    }
    private static BigDecimal money(String value){if(value==null||!value.matches("[0-9]{1,9}(\\.[0-9]{1,2})?"))invalid("金额须为非负数字，最多两位小数");return new BigDecimal(value).setScale(2);}
    private static LocalDate date(String value){try{if(value==null||!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))invalid("日期格式不正确");return LocalDate.parse(value);}catch(java.time.DateTimeException e){invalid("日期不正确");return null;}}
    private static String text(String value,int max){String t=value==null?"":value.trim();if(t.length()>max)invalid("约定文字超出长度限制");return t;}
    private static void invalid(String message){throw new IouBusinessException(400,"INVALID_CONDITIONS",message);}
}
