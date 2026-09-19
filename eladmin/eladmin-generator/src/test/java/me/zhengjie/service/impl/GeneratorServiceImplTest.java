package me.zhengjie.service.impl;

import me.zhengjie.domain.ColumnInfo;
import me.zhengjie.mapper.ColumnInfoMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeneratorServiceImplTest {

    @Test
    void queryAppliesSafeDefaultsFromDatabaseMetadata() {
        ColumnInfo id = column("id", "bigint");
        id.setKeyType("PRI");
        id.setExtra("auto_increment");

        ColumnInfo quantity = column("core_qty", "decimal");
        ColumnInfo lockedAt = column("locked_at", "datetime");

        ColumnInfo createTime = column("create_time", "datetime");
        createTime.setColumnDefault("CURRENT_TIMESTAMP");

        ColumnInfoMapper mapper = mock(ColumnInfoMapper.class);
        when(mapper.getColumns("binance_spot_core_position"))
                .thenReturn(Arrays.asList(id, quantity, lockedAt, createTime));

        GeneratorServiceImpl service = new GeneratorServiceImpl(mapper);
        List<ColumnInfo> result = service.query("binance_spot_core_position");

        assertFalse(result.get(0).getNotNull());
        assertFalse(result.get(0).getListShow());
        assertFalse(result.get(0).getFormShow());
        assertEquals("Number", result.get(1).getFormType());
        assertEquals("Date", result.get(2).getFormType());
        assertFalse(result.get(3).getNotNull());
        assertFalse(result.get(3).getFormShow());
        assertEquals("Date", result.get(3).getFormType());
    }

    private ColumnInfo column(String name, String type) {
        ColumnInfo column = new ColumnInfo();
        column.setColumnName(name);
        column.setColumnType(type);
        column.setKeyType("");
        column.setExtra("");
        column.setNotNull(true);
        column.setListShow(true);
        column.setFormShow(true);
        return column;
    }
}
