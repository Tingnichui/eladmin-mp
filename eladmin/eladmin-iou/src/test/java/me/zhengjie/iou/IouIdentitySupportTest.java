package me.zhengjie.iou;

import me.zhengjie.iou.service.IouIdentitySupport;
import me.zhengjie.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IouIdentitySupportTest {
    @Test void sameIdentityHasStableHashWithConfiguredKey() {
        IouIdentitySupport first=new IouIdentitySupport("test-encryption-password","test-only-hmac-key-32-characters-minimum");
        IouIdentitySupport second=new IouIdentitySupport("test-encryption-password","test-only-hmac-key-32-characters-minimum");
        String id="11010119900101001X";
        assertEquals(first.hash(id),second.hash(" 11010119900101001x "));
        assertTrue(first.hash(id).matches("[a-f0-9]{64}"));
        assertNotEquals(first.hash(id),first.hash("110101199001010010"));
    }
    @Test void insufficientKeyFailsWithoutExposingConfigurationName() {
        BadRequestException failure=assertThrows(BadRequestException.class,
            ()->new IouIdentitySupport("test-encryption-password","").hash("110101199001010010"));
        assertFalse(failure.getMessage().contains("IOU_IDENTITY_HMAC_KEY"));
    }
}
