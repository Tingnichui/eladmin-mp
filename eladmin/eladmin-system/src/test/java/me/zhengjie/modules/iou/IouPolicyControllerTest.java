package me.zhengjie.modules.iou;

import me.zhengjie.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IouPolicyControllerTest {
    @Test void allPoliciesHaveRealContactDetailsAndVersion() throws Exception {
        IouPolicyController controller=new IouPolicyController("2026-10-10-v1");
        assertEquals("2026-10-10-v1",controller.policies().get("version"));
        for (String kind:new String[]{"agreement","privacy","identity"}) {
            String body=controller.policy(kind).getBody();
            assertNotNull(body);assertTrue(body.contains("lexiangjie@qq.com"));assertTrue(body.contains("乐享借"));
            assertFalse(body.contains("【"));assertFalse(body.contains("测试版"));
        }
    }
    @Test void arbitraryResourcePathsAreRejected() {
        assertThrows(BadRequestException.class,()->new IouPolicyController("v1").policy("../config/application.yml"));
    }
}
