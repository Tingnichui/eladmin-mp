/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package me.zhengjie.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.template.*;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.domain.GenConfig;
import me.zhengjie.domain.ColumnInfo;
import org.springframework.util.ObjectUtils;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;
import static me.zhengjie.utils.FileUtil.SYS_TEM_DIR;

/**
 * 代码生成
 *
 * @author Zheng Jie
 * @date 2019-01-02
 */
@Slf4j
@SuppressWarnings({"unchecked", "all"})
public class GenUtil {

    private static final String TIMESTAMP = "Timestamp";

    private static final String BIGDECIMAL = "BigDecimal";

    private static final String LONG = "Long";

    private static final String STRING = "String";

    public static final String PK = "PRI";

    public static final String EXTRA = "auto_increment";

    /**
     * 获取后端代码模板名称
     *
     * @return List
     */
    private static List<String> getAdminTemplateNames() {
        List<String> templateNames = new ArrayList<>();
        templateNames.add("Entity");
        templateNames.add("Controller");
        templateNames.add("QueryCriteria");
        templateNames.add("Service");
        templateNames.add("ServiceImpl");
        templateNames.add("Mapper");
        templateNames.add("Mapper-xml");
        return templateNames;
    }

    /**
     * 获取前端代码模板名称
     *
     * @return List
     */
    private static List<String> getFrontTemplateNames() {
        List<String> templateNames = new ArrayList<>();
        templateNames.add("index");
        templateNames.add("api");
        return templateNames;
    }

    public static List<Map<String, Object>> preview(List<ColumnInfo> columns, GenConfig genConfig) {
        Map<String, Object> genMap = getGenMap(columns, genConfig);
        List<Map<String, Object>> genList = new ArrayList<>();
        // 获取后端模版
        List<String> templates = getAdminTemplateNames();
        TemplateEngine engine = TemplateUtil.createEngine(new TemplateConfig("template", TemplateConfig.ResourceMode.CLASSPATH));
        for (String templateName : templates) {
            Map<String, Object> map = new HashMap<>(1);
            Template template = engine.getTemplate("admin/" + templateName + ".ftl");
            map.put("content", template.render(genMap));
            map.put("name", templateName.replace("-xml", ".xml"));
            genList.add(map);
        }
        // 获取前端模版
        templates = getFrontTemplateNames();
        for (String templateName : templates) {
            Map<String, Object> map = new HashMap<>(1);
            Template template = engine.getTemplate("front/" + templateName + ".ftl");
            map.put(templateName, template.render(genMap));
            map.put("content", template.render(genMap));
            map.put("name", templateName);
            genList.add(map);
        }
        return genList;
    }

    public static String download(List<ColumnInfo> columns, GenConfig genConfig) throws IOException {
        // 拼接的路径：/tmpeladmin-gen-temp/，这个路径在Linux下需要root用户才有权限创建,非root用户会权限错误而失败，更改为： /tmp/eladmin-gen-temp/
        // String tempPath =SYS_TEM_DIR + "eladmin-gen-temp" + File.separator + genConfig.getTableName() + File.separator;
        String tempPath = SYS_TEM_DIR + "eladmin-gen-temp" + File.separator + genConfig.getTableName() + File.separator;
        Map<String, Object> genMap = getGenMap(columns, genConfig);
        TemplateEngine engine = TemplateUtil.createEngine(new TemplateConfig("template", TemplateConfig.ResourceMode.CLASSPATH));
        // 生成后端代码
        List<String> templates = getAdminTemplateNames();
        for (String templateName : templates) {
            Template template = engine.getTemplate("admin/" + templateName + ".ftl");
            String filePath = getAdminFilePath(templateName, genConfig, genMap.get("className").toString(), tempPath + "eladmin" + File.separator);
            assert filePath != null;
            File file = new File(filePath);
            // 如果非覆盖生成
            if (!genConfig.getCover() && FileUtil.exist(file)) {
                continue;
            }
            // 生成代码
            genFile(file, template, genMap);
        }
        // 生成前端代码
        templates = getFrontTemplateNames();
        for (String templateName : templates) {
            Template template = engine.getTemplate("front/" + templateName + ".ftl");
            String path = tempPath + "eladmin-web" + File.separator;
            String apiPath = path + "src" + File.separator + "api" + File.separator;
            String srcPath = path + "src" + File.separator + "views" + File.separator + genMap.get("changeClassName").toString() + File.separator;
            String filePath = getFrontFilePath(templateName, apiPath, srcPath, genMap.get("changeClassName").toString());
            assert filePath != null;
            File file = new File(filePath);
            // 如果非覆盖生成
            if (!genConfig.getCover() && FileUtil.exist(file)) {
                continue;
            }
            // 生成代码
            genFile(file, template, genMap);
        }
        return tempPath;
    }

    public static void generatorCode(List<ColumnInfo> columnInfos, GenConfig genConfig) throws IOException {
        Map<String, Object> genMap = getGenMap(columnInfos, genConfig);
        TemplateEngine engine = TemplateUtil.createEngine(new TemplateConfig("template", TemplateConfig.ResourceMode.CLASSPATH));
        // 生成后端代码
        List<String> templates = getAdminTemplateNames();
        for (String templateName : templates) {
            Template template = engine.getTemplate("admin/" + templateName + ".ftl");
            String rootPath = System.getProperty("user.dir");
            String filePath = getAdminFilePath(templateName, genConfig, genMap.get("className").toString(), rootPath);

            assert filePath != null;
            File file = new File(filePath);

            // 如果非覆盖生成
            if (!genConfig.getCover() && FileUtil.exist(file)) {
                continue;
            }
            // 生成代码
            genFile(file, template, genMap);
        }

        // 生成前端代码
        templates = getFrontTemplateNames();
        for (String templateName : templates) {
            Template template = engine.getTemplate("front/" + templateName + ".ftl");
            String filePath = getFrontFilePath(templateName, genConfig.getApiPath(), genConfig.getPath(), genMap.get("changeClassName").toString());

            assert filePath != null;
            File file = new File(filePath);

            // 如果非覆盖生成
            if (!genConfig.getCover() && FileUtil.exist(file)) {
                continue;
            }
            // 生成代码
            genFile(file, template, genMap);
        }
    }

    // 获取模版数据
    private static Map<String, Object> getGenMap(List<ColumnInfo> columnInfos, GenConfig genConfig) {
        // 存储模版字段数据
        Map<String, Object> genMap = new HashMap<>(16);
        // 接口别名
        genMap.put("apiAlias", genConfig.getApiAlias());
        // 包名称
        genMap.put("package", genConfig.getPack());
        // 模块名称
        genMap.put("moduleName", genConfig.getModuleName());
        // 作者
        genMap.put("author", genConfig.getAuthor());
        // 创建日期
        genMap.put("date", LocalDate.now().toString());
        // 表名
        genMap.put("tableName", genConfig.getTableName());
        // 大写开头的类名
        String className = StringUtils.toCapitalizeCamelCase(genConfig.getTableName());
        // 小写开头的类名
        String changeClassName = StringUtils.toCamelCase(genConfig.getTableName());
        // 判断是否去除表前缀
        if (StringUtils.isNotEmpty(genConfig.getPrefix())) {
            className = StringUtils.toCapitalizeCamelCase(StrUtil.removePrefix(genConfig.getTableName(), genConfig.getPrefix()));
            changeClassName = StringUtils.toCamelCase(StrUtil.removePrefix(genConfig.getTableName(), genConfig.getPrefix()));
            changeClassName = StringUtils.uncapitalize(changeClassName);
        }
        // 保存类名
        genMap.put("className", className);
        // 保存小写开头的类名
        genMap.put("changeClassName", changeClassName);
        // 存在 Timestamp 字段
        genMap.put("hasTimestamp", false);
        // 查询类中存在 Timestamp 字段
        genMap.put("queryHasTimestamp", false);
        // 存在 BigDecimal 字段
        genMap.put("hasBigDecimal", false);
        // 存在 Long 字段
        genMap.put("hasLong", false);
        // 查询类中存在 BigDecimal 字段
        genMap.put("queryHasBigDecimal", false);
        // 是否需要创建查询
        genMap.put("hasQuery", false);
        // 自增主键
        genMap.put("auto", false);
        // 存在字典
        genMap.put("hasDict", false);
        // 存在日期注解
        genMap.put("hasDateAnnotation", false);
        // 存储主键字段名
        genMap.put("pkIdName", "none");
        // 存储符号
        genMap.put("symbol", "#");
        // 保存字段信息
        List<Map<String, Object>> columns = new ArrayList<>();
        // 保存查询字段的信息
        List<Map<String, Object>> queryColumns = new ArrayList<>();
        // 存储字典信息
        List<String> dicts = new ArrayList<>();
        // 存储 between 信息
        List<Map<String, Object>> betweens = new ArrayList<>();
        // 存储不为空的字段信息
        List<Map<String, Object>> isNotNullColumns = new ArrayList<>();
        // 统计导出表头，避免重复备注导致 Map key 覆盖
        Map<String, Integer> exportNameCount = getExportNameCount(columnInfos);

        for (ColumnInfo column : columnInfos) {
            Map<String, Object> listMap = new HashMap<>(16);
            // 字段描述
            listMap.put("remark", column.getRemark());
            // 字段类型
            listMap.put("columnKey", column.getKeyType());
            // 主键类型
            String colType = ColUtil.cloToJava(column.getColumnType());
            // 小写开头的字段名
            String changeColumnName = StringUtils.toCamelCase(column.getColumnName());
            // 大写开头的字段名
            String capitalColumnName = StringUtils.toCapitalizeCamelCase(column.getColumnName());
            // 是否是框架自动维护字段
            boolean autoMaintain = isAutoMaintainColumn(column);
            // 是否是 Long 类型
            boolean longType = LONG.equals(colType);
            // 查询接收类型，Long 使用 String 避免前端 JS 精度丢失
            String queryColumnType = longType ? STRING : colType;
            // 是否必填，自动维护字段不生成阻断新增的校验
            boolean notNull = Boolean.TRUE.equals(column.getNotNull()) && !autoMaintain;
            // 是否表单显示，自动维护字段默认只读维护，不进入新增/编辑表单
            boolean formShow = !autoMaintain && !Boolean.FALSE.equals(column.getFormShow());
            String exportName = getExportName(column, changeColumnName, exportNameCount);
            if (PK.equals(column.getKeyType())) {
                // 存储主键类型
                genMap.put("pkColumnType", colType);
                // 存储小写开头的字段名
                genMap.put("pkChangeColName", changeColumnName);
                // 存储大写开头的字段名
                genMap.put("pkCapitalColName", capitalColumnName);
                // 存储主键字段名
                genMap.put("pkIdName", column.getColumnName());
            }
            // 是否存在 Timestamp 类型的字段
            if (TIMESTAMP.equals(colType)) {
                genMap.put("hasTimestamp", true);
            }
            // 是否存在 BigDecimal 类型的字段
            if (BIGDECIMAL.equals(colType)) {
                genMap.put("hasBigDecimal", true);
            }
            // 是否存在 Long 类型的字段
            if (longType) {
                genMap.put("hasLong", true);
            }
            // 主键是否自增
            if (EXTRA.equals(column.getExtra())) {
                genMap.put("auto", true);
            }
            // 主键存在字典
            if (StringUtils.isNotBlank(column.getDictName())) {
                genMap.put("hasDict", true);
                if(!dicts.contains(column.getDictName()))
                    dicts.add(column.getDictName());
            }

            // 存储字段类型
            listMap.put("columnType", colType);
            // 查询字段类型
            listMap.put("queryColumnType", queryColumnType);
            // 是否按字符串查询
            listMap.put("queryStringType", STRING.equals(queryColumnType));
            // 是否 Long 类型
            listMap.put("longType", longType);
            // 是否 String 类型
            listMap.put("stringType", STRING.equals(colType));
            // 是否自动维护字段
            listMap.put("autoMaintain", autoMaintain);
            // 导出表头，重复备注时自动追加字段名避免覆盖
            listMap.put("exportName", exportName);
            // 存储字原始段名称
            listMap.put("columnName", column.getColumnName());
            // 不为空
            listMap.put("istNotNull", notNull);
            // 字段列表显示
            listMap.put("columnShow", !Boolean.FALSE.equals(column.getListShow()));
            // 表单显示
            listMap.put("formShow", formShow);
            // 表单组件类型
            listMap.put("formType", StringUtils.isNotBlank(column.getFormType()) ? column.getFormType() : "Input");
            // 小写开头的字段名称
            listMap.put("changeColumnName", changeColumnName);
            //大写开头的字段名称
            listMap.put("capitalColumnName", capitalColumnName);
            // 字典名称
            listMap.put("dictName", column.getDictName());
            // 添加非空字段信息
            if (notNull) {
                isNotNullColumns.add(listMap);
            }
            // 判断是否有查询，如有则把查询的字段set进columnQuery
            if (!StringUtils.isBlank(column.getQueryType())) {
                // 查询类型
                listMap.put("queryType", column.getQueryType());
                // 是否存在查询
                genMap.put("hasQuery", true);
                if (TIMESTAMP.equals(queryColumnType)) {
                    // 查询中存储 Timestamp 类型
                    genMap.put("queryHasTimestamp", true);
                }
                if (BIGDECIMAL.equals(queryColumnType)) {
                    // 查询中存储 BigDecimal 类型
                    genMap.put("queryHasBigDecimal", true);
                }
                if ("between".equalsIgnoreCase(column.getQueryType())) {
                    betweens.add(listMap);
                } else {
                    // 添加到查询列表中
                    queryColumns.add(listMap);
                }
            }
            // 添加到字段列表中
            columns.add(listMap);
        }
        // 保存字段列表
        genMap.put("columns", columns);
        // 保存查询列表
        genMap.put("queryColumns", queryColumns);
        // 保存字段列表
        genMap.put("dicts", dicts);
        // 保存查询列表
        genMap.put("betweens", betweens);
        // 保存非空字段信息
        genMap.put("isNotNullColumns", isNotNullColumns);
        return genMap;
    }

    private static Map<String, Integer> getExportNameCount(List<ColumnInfo> columnInfos) {
        Map<String, Integer> exportNameCount = new HashMap<>(16);
        for (ColumnInfo column : columnInfos) {
            if (PK.equals(column.getKeyType())) {
                continue;
            }
            String changeColumnName = StringUtils.toCamelCase(column.getColumnName());
            String exportName = getExportBaseName(column, changeColumnName);
            exportNameCount.put(exportName, exportNameCount.getOrDefault(exportName, 0) + 1);
        }
        return exportNameCount;
    }

    private static String getExportName(ColumnInfo column, String changeColumnName, Map<String, Integer> exportNameCount) {
        String exportName = getExportBaseName(column, changeColumnName);
        if (exportNameCount.getOrDefault(exportName, 0) > 1) {
            return exportName + "(" + changeColumnName + ")";
        }
        return exportName;
    }

    private static String getExportBaseName(ColumnInfo column, String changeColumnName) {
        return StringUtils.isNotBlank(column.getRemark()) ? column.getRemark() : changeColumnName;
    }

    public static boolean isAutoMaintainColumn(ColumnInfo column) {
        String columnName = column.getColumnName();
        if (columnName == null) {
            return false;
        }
        String name = columnName.toLowerCase(Locale.ROOT);
        if ("create_time".equals(name) || "created_time".equals(name)
                || "update_time".equals(name) || "updated_time".equals(name)
                || "create_by".equals(name) || "created_by".equals(name)
                || "update_by".equals(name) || "updated_by".equals(name)) {
            return true;
        }
        return column.getExtra() != null && column.getExtra().toLowerCase(Locale.ROOT).contains("on update");
    }

    /**
     * 定义后端文件路径以及名称
     */
    private static String getAdminFilePath(String templateName, GenConfig genConfig, String className, String rootPath) {
        String projectPath = rootPath + File.separator + genConfig.getModuleName();
        String packagePath = projectPath + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator;
        String mpXmlPath = projectPath + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator;
        if (!ObjectUtils.isEmpty(genConfig.getPack())) {
            packagePath += genConfig.getPack().replace(".", File.separator) + File.separator;
        }

        if ("Entity".equals(templateName)) {
            return packagePath + "domain" + File.separator + className + ".java";
        }

        if ("Controller".equals(templateName)) {
            return packagePath + "rest" + File.separator + className + "Controller.java";
        }

        if ("Service".equals(templateName)) {
            return packagePath + "service" + File.separator + className + "Service.java";
        }

        if ("ServiceImpl".equals(templateName)) {
            return packagePath + "service" + File.separator + "impl" + File.separator + className + "ServiceImpl.java";
        }

        if ("QueryCriteria".equals(templateName)) {
            return packagePath + "domain" + File.separator + "dto" + File.separator + className + "QueryCriteria.java";
        }

        if ("Mapper".equals(templateName)) {
            return packagePath + "mapper" + File.separator + className + "Mapper.java";
        }

        if ("Mapper-xml".equals(templateName)) {
            return mpXmlPath + "mapper" + File.separator + className + "Mapper.xml";
        }

        return null;
    }

    /**
     * 定义前端文件路径以及名称
     */
    private static String getFrontFilePath(String templateName, String apiPath, String path, String apiName) {

        if ("api".equals(templateName)) {
            return apiPath + File.separator + apiName + ".js";
        }

        if ("index".equals(templateName)) {
            return path + File.separator + "index.vue";
        }

        return null;
    }

    private static void genFile(File file, Template template, Map<String, Object> map) throws IOException {
        // 生成目标文件
        FileUtil.touch(file);
        try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            template.render(map, writer);
        } catch (TemplateException e) {
            throw new RuntimeException(e);
        }
    }
}
