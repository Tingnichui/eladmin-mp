package me.zhengjie.iou.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
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
    private String serviceFeePolicy = "DISABLED";
    @org.springframework.beans.factory.annotation.Value("${iou.notes.seal-key:${IOU_NOTE_SEAL_KEY:}}")
    private String sealKey = "";
    private String shareBaseUrl = "";
    private String storagePath = System.getProperty("user.home") + "/.eladmin/iou-agreements";
    public boolean canSign() {
        return creationEnabled && signingEnabled && "DISABLED".equals(serviceFeePolicy)
            && sealKey != null && sealKey.length() >= 32;
    }
    public Map<String, Object> view() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("creationEnabled", creationEnabled); r.put("signingEnabled", canSign());
        r.put("signatureMethod", "HANDWRITTEN"); r.put("serviceFeePolicy", serviceFeePolicy);
        r.put("repaymentMethods", Arrays.asList("once", "annuity", "principal", "interest", "flat", "custom"));
        r.put("cycleMonths", Arrays.asList(1, 3)); r.put("minInstallments", 2); r.put("maxInstallments", 60);
        r.put("principalMin", "0.01"); r.put("principalMax", principalMax.toPlainString());
        r.put("rateMax", rateMax.toPlainString()); r.put("maxTermDays", maxTermDays);
        r.put("calculationVersion", IouRepaymentCalculator.VERSION); r.put("contractTemplateVersion", "handwritten-v1");
        r.put("prepaymentRules", Arrays.asList("actual", "agreement"));
        return r;
    }
    @Override public String toString() { return "IouNoteOptions"; }
}
