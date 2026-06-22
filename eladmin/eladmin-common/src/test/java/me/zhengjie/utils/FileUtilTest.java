package me.zhengjie.utils;

import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static me.zhengjie.utils.FileUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileUtilTest {

    @Test
    public void testToFile() {
        long retval = toFile(new MockMultipartFile("foo", (byte[]) null)).getTotalSpace();
        assertEquals(500695072768L, retval);
    }

    @Test
    public void testGetExtensionName() {
        assertEquals("foo", getExtensionName("foo"));
        assertEquals("exe", getExtensionName("bar.exe"));
    }

    @Test
    public void testGetFileNameNoEx() {
        assertEquals("foo", getFileNameNoEx("foo"));
        assertEquals("bar", getFileNameNoEx("bar.txt"));
    }

    @Test
    public void testGetSize() {
        assertEquals("1000B   ", getSize(1000));
        assertEquals("1.00KB   ", getSize(1024));
        assertEquals("1.00MB   ", getSize(1048576));
        assertEquals("1.00GB   ", getSize(1073741824));
    }

    @Test
    public void testExcelBatchWriter() throws Exception {
        File file = Files.createTempFile("excel-batch-writer", ".xlsx").toFile();
        Files.deleteIfExists(file.toPath());
        BigExcelWriter writer = ExcelUtil.getBigWriter(file);
        try {
            FileUtil.ExcelBatchWriter batchWriter =
                    new FileUtil.ExcelBatchWriter(writer, Arrays.asList("名称"), 3);
            List<Map<String, Object>> rows = new ArrayList<>();
            rows.add(row("正常"));
            rows.add(row("=SUM(A1:A2)"));
            rows.add(row("+公式"));
            rows.add(row("-公式"));
            rows.add(row("@公式"));
            batchWriter.write(rows);

            assertEquals(2, writer.getSheetCount());
            assertEquals(4, writer.getSheets().get(0).getPhysicalNumberOfRows());
            assertEquals(3, writer.getSheets().get(1).getPhysicalNumberOfRows());
            assertEquals("'=SUM(A1:A2)", writer.getSheets().get(0).getRow(2).getCell(0).getStringCellValue());
            assertEquals("'-公式", writer.getSheets().get(1).getRow(1).getCell(0).getStringCellValue());
            assertEquals("'@公式", writer.getSheets().get(1).getRow(2).getCell(0).getStringCellValue());
        } finally {
            writer.close();
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test
    public void testExcelBatchWriterWithEmptyData() throws Exception {
        File file = Files.createTempFile("excel-empty-writer", ".xlsx").toFile();
        Files.deleteIfExists(file.toPath());
        BigExcelWriter writer = ExcelUtil.getBigWriter(file);
        try {
            new FileUtil.ExcelBatchWriter(writer, Arrays.asList("名称"), 3);
            assertEquals(1, writer.getSheetCount());
            assertEquals("名称", writer.getSheet().getRow(0).getCell(0).getStringCellValue());
        } finally {
            writer.close();
            Files.deleteIfExists(file.toPath());
        }
    }

    private Map<String, Object> row(String value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("名称", value);
        return row;
    }
}
