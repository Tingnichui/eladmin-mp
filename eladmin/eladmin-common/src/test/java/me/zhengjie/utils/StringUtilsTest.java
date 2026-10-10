package me.zhengjie.utils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static me.zhengjie.utils.StringUtils.getIp;
import static me.zhengjie.utils.StringUtils.toCamelCase;
import static me.zhengjie.utils.StringUtils.toCapitalizeCamelCase;
import static me.zhengjie.utils.StringUtils.toUnderScoreCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StringUtilsTest {

    @Test
    public void testToCamelCase() {
        assertNull(toCamelCase(null));
    }

    @Test
    public void testToCapitalizeCamelCase() {
        assertNull(StringUtils.toCapitalizeCamelCase(null));
        assertEquals("HelloWorld", toCapitalizeCamelCase("hello_world"));
    }

    @Test
    public void testToUnderScoreCase() {
        assertNull(StringUtils.toUnderScoreCase(null));
        assertEquals("hello_world", toUnderScoreCase("helloWorld"));
        assertEquals("\u0000\u0000", toUnderScoreCase("\u0000\u0000"));
        assertEquals("\u0000_a", toUnderScoreCase("\u0000A"));
    }

    @Test
    public void testGetWeekDay() {
        assertEquals("Sun", StringUtils.getWeekDay(
                new GregorianCalendar(2024, Calendar.JANUARY, 7).getTime()));
    }

    @Test
    public void testGetIP() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-forwarded-for", "192.0.2.10, 192.0.2.11");
        assertEquals("192.0.2.10", getIp(request));
    }
}
