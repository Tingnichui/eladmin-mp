package me.zhengjie.iou.service;

import me.zhengjie.iou.mapper.IouNoteNumberMapper;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IouNoteNumberServiceTest {
    @Test void timeCurveMatchesExamplesAndNeverGoesBackwards() {
        int[] hours = {0, 6, 9, 12, 18, 23};
        int[] values = {100, 220, 820, 2020, 4420, 5420};
        for (int i = 0; i < hours.length; i++) {
            assertEquals(values[i], IouNoteNumberService.timeValue(hours[i] * 3600));
        }
        int prior = 100;
        for (int second = 0; second < 86400; second++) {
            int value = IouNoteNumberService.timeValue(second);
            assertTrue(value >= prior && value - prior <= 1);
            prior = value;
        }
        assertThrows(IllegalArgumentException.class, () -> IouNoteNumberService.timeValue(-1));
        assertThrows(IllegalArgumentException.class, () -> IouNoteNumberService.timeValue(86400));
    }

    @Test void allocationUsesShanghaiDateAndAdvancesWithinSameTimeSlot() {
        IouNoteNumberMapper mapper = mock(IouNoteNumberMapper.class);
        when(mapper.lockDay("2026-10-11")).thenReturn(0L, 2020L, 2021L);
        when(mapper.saveValue(anyString(), anyLong())).thenReturn(1);
        IouNoteNumberService service = new IouNoteNumberService(mapper,
            Clock.fixed(Instant.parse("2026-10-11T04:00:00Z"), ZoneOffset.UTC));
        assertEquals("JT20261011-002020", service.next());
        assertEquals("JT20261011-002021", service.next());
        assertEquals("JT20261011-002022", service.next());
        verify(mapper, times(3)).ensureDay("2026-10-11");
        verify(mapper).saveValue("2026-10-11", 2022L);
    }

    @Test void midnightStartsNewDayAndPreviouslyAllocatedValueSurvivesClockRollback() {
        IouNoteNumberMapper mapper = mock(IouNoteNumberMapper.class);
        when(mapper.lockDay("2026-10-12")).thenReturn(0L, 8000L);
        when(mapper.saveValue(anyString(), anyLong())).thenReturn(1);
        IouNoteNumberService service = new IouNoteNumberService(mapper,
            Clock.fixed(Instant.parse("2026-10-11T16:00:00Z"), ZoneOffset.UTC));
        assertEquals("JT20261012-000100", service.next());
        assertEquals("JT20261012-008001", service.next());
    }

    @Test void counterWriteFailureAndCapacityDoNotReturnAnUnsavedNumber() {
        IouNoteNumberMapper mapper = mock(IouNoteNumberMapper.class);
        when(mapper.lockDay(anyString())).thenReturn(999999L, 0L, null);
        IouNoteNumberService service = new IouNoteNumberService(mapper,
            Clock.fixed(Instant.parse("2026-10-11T04:00:00Z"), ZoneOffset.UTC));
        assertThrows(IllegalStateException.class, service::next);
        verify(mapper, never()).saveValue(anyString(), anyLong());
        assertThrows(IllegalStateException.class, service::next);
        assertThrows(IllegalStateException.class, service::next);
    }
}
