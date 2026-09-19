package me.zhengjie.utils;

import me.zhengjie.domain.ColumnInfo;
import me.zhengjie.domain.GenConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GenUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesBackendRootFromFrontendPathWhenRuntimeDirectoryIsNested() throws Exception {
        Path backendRoot = Files.createDirectories(tempDir.resolve("eladmin"));
        Path module = Files.createDirectories(backendRoot.resolve("eladmin-invest"));
        Files.write(module.resolve("pom.xml"), "<project/>".getBytes(StandardCharsets.UTF_8));
        Path runtimeDir = Files.createDirectories(backendRoot.resolve("eladmin-system"));
        Path viewPath = Files.createDirectories(
                tempDir.resolve("eladmin-web/src/views/invest/corePosition"));

        GenConfig config = new GenConfig("binance_spot_core_position");
        config.setModuleName("eladmin-invest");
        config.setPath(viewPath.toString());

        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", runtimeDir.toString());
            assertEquals(backendRoot.toString(), GenUtil.resolveBackendRoot(config));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void rejectsPathLikeModuleName() {
        GenConfig config = new GenConfig("binance_spot_core_position");
        config.setModuleName("eladmin/eladmin-invest");
        assertThrows(Exception.class, () -> GenUtil.resolveBackendRoot(config));
    }

    @Test
    void previewUsesNumberControlForDecimalFields() {
        ColumnInfo id = column("id", "bigint", "PRI", "auto_increment", "主键");
        id.setNotNull(false);
        id.setListShow(false);
        id.setFormShow(false);
        ColumnInfo quantity = column("core_qty", "decimal", "", "", "底仓数量");
        quantity.setFormType("Number");

        GenConfig config = new GenConfig("binance_spot_core_position");
        config.setApiAlias("现货底仓");
        config.setPack("me.zhengjie.invest");
        config.setModuleName("eladmin-invest");
        config.setAuthor("test");
        config.setCover(false);

        List<Map<String, Object>> preview = GenUtil.preview(Arrays.asList(id, quantity), config);
        String index = preview.stream()
                .filter(item -> "index".equals(item.get("name")))
                .map(item -> String.valueOf(item.get("content")))
                .findFirst()
                .orElseThrow(AssertionError::new);

        assertTrue(index.contains("<el-input-number v-model=\"form.coreQty\""));
    }

    private ColumnInfo column(String name, String type, String key, String extra, String remark) {
        ColumnInfo column = new ColumnInfo();
        column.setTableName("binance_spot_core_position");
        column.setColumnName(name);
        column.setColumnType(type);
        column.setKeyType(key);
        column.setExtra(extra);
        column.setRemark(remark);
        column.setNotNull(true);
        column.setListShow(true);
        column.setFormShow(true);
        column.setFormType("Input");
        return column;
    }
}
