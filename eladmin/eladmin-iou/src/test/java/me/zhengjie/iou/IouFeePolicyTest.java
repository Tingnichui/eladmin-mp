package me.zhengjie.iou;

import me.zhengjie.iou.service.IouNoteOptions;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class IouFeePolicyTest {
    @Test void proportionUsesPrincipalRoundsToCentsAndAppliesLimits() {
        IouNoteOptions options=new IouNoteOptions();options.setServiceFeePolicy("ENABLED");options.setServiceFeeRate(new BigDecimal("0.001"));
        assertEquals("10.00",options.fee(new BigDecimal("10000")).get("amount"));
        assertEquals("0.02",options.fee(new BigDecimal("15")).get("amount"));
        assertEquals("0.00",options.fee(new BigDecimal("0.01")).get("amount"));
        options.setServiceFeeMin(new BigDecimal("1.00"));options.setServiceFeeMax(new BigDecimal("100.00"));
        assertEquals("1.00",options.fee(new BigDecimal("100")).get("amount"));
        assertEquals("100.00",options.fee(new BigDecimal("1000000")).get("amount"));
    }
    @Test void invalidRateOrBoundsCannotPresentFreeOrEnableSigning() {
        IouNoteOptions options=new IouNoteOptions();options.setServiceFeePolicy("ENABLED");options.setServiceFeeRate(null);
        assertEquals("UNCONFIGURED",options.fee(BigDecimal.TEN).get("policy"));assertNull(options.fee(BigDecimal.TEN).get("amount"));
        options.setServiceFeeRate(BigDecimal.ONE);options.setServiceFeeMin(BigDecimal.TEN);options.setServiceFeeMax(BigDecimal.ONE);
        assertFalse(options.feeConfigured());assertFalse(options.canSign());
        options.setServiceFeePolicy("DISABLED");assertEquals("0.00",options.fee(BigDecimal.TEN).get("amount"));
    }
    @Test void defaultRateIsTwoPointFivePercent(){IouNoteOptions options=new IouNoteOptions();assertEquals("250.00",options.fee(new BigDecimal("10000.00")).get("amount"));assertEquals("2.51",options.fee(new BigDecimal("100.20")).get("amount"));}
}
