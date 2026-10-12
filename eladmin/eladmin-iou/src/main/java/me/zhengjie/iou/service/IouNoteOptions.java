package me.zhengjie.iou.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Data
@Component
@ConfigurationProperties(prefix = "iou.notes")
public class IouNoteOptions {
    private boolean creationEnabled = true;
    private boolean signingEnabled = false;
    private BigDecimal principalMax = new BigDecimal("100000000.00");
    private BigDecimal rateMax = new BigDecimal("1.00000000");
    private int maxTermDays = 3650;
    private String serviceFeePolicy = "ENABLED";
    private BigDecimal serviceFeeRate = new BigDecimal("0.025");
    private BigDecimal serviceFeeMin = BigDecimal.ZERO;
    private BigDecimal serviceFeeMax;
    private String serviceFeeVersion = "principal-ratio-v1";
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private IouPaymentOptions paymentOptions;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private IouNativeWechatOptions nativeWechatOptions;
    @org.springframework.beans.factory.annotation.Value("${iou.notes.seal-key:${IOU_NOTE_SEAL_KEY:}}")
    private String sealKey = "";
    private String shareBaseUrl = "";
    private String storagePath = System.getProperty("user.home") + "/.eladmin/iou-agreements";
    public boolean canSign() {
        return basicSigningReady() && ("DISABLED".equals(serviceFeePolicy)
            || "ENABLED".equals(serviceFeePolicy) && feeConfigured() && nativeWechatOptions != null && nativeWechatOptions.ready())
            ;
    }
    public boolean basicSigningReady(){return creationEnabled&&signingEnabled&&sealKey!=null&&sealKey.length()>=32;}
    public boolean feeConfigured() {
        return serviceFeeRate != null && serviceFeeRate.signum() > 0 && serviceFeeRate.compareTo(BigDecimal.ONE) <= 0
            && serviceFeeMin != null && serviceFeeMin.signum() >= 0 && serviceFeeMin.scale() <= 2
            && (serviceFeeMax == null || serviceFeeMax.signum() > 0 && serviceFeeMax.scale() <= 2 && serviceFeeMax.compareTo(serviceFeeMin) >= 0)
            && serviceFeeVersion != null && !serviceFeeVersion.trim().isEmpty();
    }
    public Map<String,Object> fee(BigDecimal principal) {
        Map<String,Object> r = new LinkedHashMap<>();
        boolean disabled = "DISABLED".equals(serviceFeePolicy);
        boolean configured = "ENABLED".equals(serviceFeePolicy) && feeConfigured();
        r.put("policy", disabled ? "DISABLED" : configured ? "ENABLED" : "UNCONFIGURED");
        r.put("payer", "borrower"); r.put("version", serviceFeeVersion);
        r.put("rate", configured ? serviceFeeRate.toPlainString() : null);
        r.put("minimum", configured ? serviceFeeMin.toPlainString() : null);
        r.put("maximum", configured && serviceFeeMax != null ? serviceFeeMax.toPlainString() : null);
        BigDecimal amount = disabled ? BigDecimal.ZERO : null;
        if (configured && principal != null) {
            amount = principal.multiply(serviceFeeRate).setScale(2, RoundingMode.HALF_UP).max(serviceFeeMin);
            if (serviceFeeMax != null) amount = amount.min(serviceFeeMax);
        }
        r.put("amount", amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        return r;
    }
    public Map<String, Object> view() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("creationEnabled", creationEnabled); r.put("signingEnabled", canSign());
        r.put("signatureMethod", "HANDWRITTEN"); r.put("serviceFeePolicy", serviceFeePolicy);
        r.put("serviceFee", fee(null)); r.put("paymentReady", nativeWechatOptions != null && nativeWechatOptions.ready());
        r.put("repaymentMethods", Arrays.asList("once", "annuity", "principal", "interest", "flat", "custom"));
        r.put("cycleMonths", Arrays.asList(1, 3)); r.put("minInstallments", 2); r.put("maxInstallments", 60);
        r.put("principalMin", "0.01"); r.put("principalMax", principalMax.toPlainString());
        r.put("rateMax", rateMax.toPlainString()); r.put("maxTermDays", maxTermDays);
        r.put("calculationVersion", IouRepaymentCalculator.VERSION); r.put("contractTemplateVersion", "handwritten-v2");
        r.put("prepaymentRules", Arrays.asList("actual", "agreement"));
        return r;
    }
    @Override public String toString() { return "IouNoteOptions"; }
}
