package me.zhengjie.iou.service;

import me.zhengjie.iou.mapper.IouNoteNumberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Time-based identifiers; their numeric portion is not an activity count. */
@Service
public class IouNoteNumberService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int[] HOURS = {0, 6, 9, 12, 18, 23, 24};
    private static final int[] VALUES = {100, 220, 820, 2020, 4420, 5420, 5460};
    private final IouNoteNumberMapper numbers;
    private final Clock clock;

    @Autowired
    public IouNoteNumberService(IouNoteNumberMapper numbers) {
        this(numbers, Clock.systemUTC());
    }

    IouNoteNumberService(IouNoteNumberMapper numbers, Clock clock) {
        this.numbers = numbers;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        ZonedDateTime now = clock.instant().atZone(ZONE);
        String day = now.toLocalDate().toString();
        numbers.ensureDay(day);
        Long last = numbers.lockDay(day);
        if (last == null) throw new IllegalStateException("借条编号计数记录不存在");
        long value = Math.max(last + 1, timeValue(now.toLocalTime().toSecondOfDay()));
        if (value > 999999) throw new IllegalStateException("当日借条编号容量已满");
        if (numbers.saveValue(day, value) != 1) throw new IllegalStateException("借条编号保存失败");
        return "JT" + now.toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE)
            + "-" + String.format(Locale.ROOT, "%06d", value);
    }

    static int timeValue(int secondOfDay) {
        if (secondOfDay < 0) throw new IllegalArgumentException("无效的日内时间");
        for (int i = 0; i < HOURS.length - 1; i++) {
            if (secondOfDay < HOURS[i + 1] * 3600) {
                return VALUES[i] + (secondOfDay - HOURS[i] * 3600)
                    * (VALUES[i + 1] - VALUES[i]) / ((HOURS[i + 1] - HOURS[i]) * 3600);
            }
        }
        throw new IllegalArgumentException("无效的日内时间");
    }
}
