package me.zhengjie.service.impl;

import me.zhengjie.domain.GenConfig;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.mapper.GenConfigMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenConfigServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void updateReusesExistingConfigAndRemovesDuplicates() throws Exception {
        Path webRoot = Files.createDirectories(tempDir.resolve("eladmin-web"));
        Path viewPath = Files.createDirectories(webRoot.resolve("src/views/invest/corePosition"));

        GenConfig existing = new GenConfig("binance_spot_core_position");
        existing.setId(12L);
        GenConfigMapper mapper = mock(GenConfigMapper.class);
        when(mapper.findByTableName("binance_spot_core_position")).thenReturn(existing);
        when(mapper.updateById(any(GenConfig.class))).thenReturn(1);

        GenConfigServiceImpl service = new GenConfigServiceImpl(mapper);
        GenConfig config = new GenConfig("binance_spot_core_position");
        config.setPath(viewPath.toString());
        config.setPack("me.zhengjie.invest");
        config.setModuleName("eladmin-invest");

        GenConfig result = service.update("binance_spot_core_position", config);

        assertEquals(12L, result.getId());
        assertEquals(webRoot.resolve("src/api").normalize().toString(), result.getApiPath());
        verify(mapper).updateById(config);
        verify(mapper, never()).insert(any(GenConfig.class));
        verify(mapper).deleteDuplicates("binance_spot_core_position", 12L);
    }

    @Test
    void updateRejectsPathOutsideEladminWebViews() {
        GenConfigMapper mapper = mock(GenConfigMapper.class);
        GenConfigServiceImpl service = new GenConfigServiceImpl(mapper);
        GenConfig config = new GenConfig("binance_spot_core_position");
        config.setPath(tempDir.resolve("other/src/views/corePosition").toString());

        assertThrows(BadRequestException.class,
                () -> service.update("binance_spot_core_position", config));
        verify(mapper, never()).findByTableName(any());
    }
}
