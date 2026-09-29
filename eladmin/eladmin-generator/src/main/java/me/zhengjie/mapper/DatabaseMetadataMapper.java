package me.zhengjie.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.zhengjie.domain.ColumnInfo;
import me.zhengjie.domain.dto.TableInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DatabaseMetadataMapper {

    IPage<TableInfo> getTables(@Param("tableName") String tableName, Page<Object> page);

    List<ColumnInfo> getColumns(@Param("tableName") String tableName);
}
