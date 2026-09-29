package me.zhengjie.service;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import me.zhengjie.domain.ColumnInfo;
import me.zhengjie.domain.dto.TableInfo;
import me.zhengjie.mapper.DatabaseMetadataMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DatabaseMetadataService {

    private final DatabaseMetadataMapper databaseMetadataMapper;

    @DS("#p0")
    public IPage<TableInfo> getTables(String dataSource, String tableName, Page<Object> page) {
        return databaseMetadataMapper.getTables(tableName, page);
    }

    @DS("#p0")
    public List<ColumnInfo> getColumns(String dataSource, String tableName) {
        return databaseMetadataMapper.getColumns(tableName);
    }
}
