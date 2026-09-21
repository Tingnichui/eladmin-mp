package me.zhengjie.aspect;

import me.zhengjie.annotation.EncryptField;
import me.zhengjie.utils.DataSecurityUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalRequestAspectTest {

    @Test
    void shouldEncryptAnnotatedFieldWithoutRecursingIntoEnumOrCycles() {
        GlobalRequestAspect aspect = new GlobalRequestAspect();
        ReflectionTestUtils.setField(aspect, "dataSecurityUtil", new StubDataSecurityUtil());

        Request request = new Request();
        request.nested.parent = request;

        assertDoesNotThrow(() -> aspect.encrypt(request));
        assertEquals("encrypted:secret", request.nested.secret);
        assertEquals(Side.SELL, request.side);
    }

    private enum Side {
        BUY,
        SELL
    }

    private static class Request {
        private static final Request STATIC_INSTANCE = new Request(false);

        private final Side side = Side.SELL;
        private final Nested nested;

        private Request() {
            this(true);
        }

        private Request(boolean initializeNested) {
            this.nested = initializeNested ? new Nested() : null;
        }
    }

    private static class Nested {
        private Request parent;

        @EncryptField
        private String secret = "secret";
    }

    private static class StubDataSecurityUtil extends DataSecurityUtil {
        @Override
        public String encrypt(String value) {
            return "encrypted:" + value;
        }
    }
}
