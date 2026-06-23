<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd" >
<mapper namespace="${package}.mapper.${className}Mapper">
    <#if columns??>
    <resultMap id="BaseResultMap" type="${package}.domain.${className}">
        <#list columns as column>
            <#if (column.columnKey!'') = 'PRI'>
        <id column="${column.columnName}" property="${column.changeColumnName}"/>
            </#if>
            <#if (column.columnKey!'') != 'PRI'>
        <result column="${column.columnName}" property="${column.changeColumnName}"/>
            </#if>
        </#list>
    </resultMap>

    <sql id="Base_Column_List">
        <#list columns as column>${column.columnName}<#if column_has_next>, </#if></#list>
    </sql>
    </#if>

    <select id="findAll" resultMap="BaseResultMap">
        SELECT
        <include refid="Base_Column_List"/>
        FROM ${tableName} t1
        <#if queryColumns??>
        WHERE 1 = 1
        <#list queryColumns as column>
            <if test="criteria.${column.changeColumnName} != null<#if column.queryStringType> and criteria.${column.changeColumnName} != ''</#if>">
            <#if column.queryType = '='>
                AND t1.${column.columnName} = ${symbol}{criteria.${column.changeColumnName}}
            </#if>
            <#if column.queryType = 'Like'>
                AND t1.${column.columnName} LIKE CONCAT('%',${symbol}{criteria.${column.changeColumnName}},'%')
            </#if>
            <#if column.queryType = '!='>
                AND t1.${column.columnName} != ${symbol}{criteria.${column.changeColumnName}}
            </#if>
            <#if column.queryType = 'NotNull'>
                AND t1.${column.columnName} IS NOT NULL
            </#if>
            <#if column.queryType = '>='>
                AND t1.${column.columnName} &gt;= ${symbol}{criteria.${column.changeColumnName}}
            </#if>
            <#if column.queryType = '<='>
                AND t1.${column.columnName} &lt;= ${symbol}{criteria.${column.changeColumnName}}
            </#if>
            </if>
        </#list>
        <#if betweens??>
            <#list betweens as column>
            <if test="criteria.${column.changeColumnName} != null and criteria.${column.changeColumnName}.size() > 1">
                AND t1.${column.columnName} BETWEEN ${symbol}{criteria.${column.changeColumnName}[0]} AND ${symbol}{criteria.${column.changeColumnName}[1]}
            </if>
            </#list>
        </#if>
        </#if>
        <#if pkIdName != 'none'>
        ORDER BY t1.${pkIdName} DESC
        </#if>
    </select>
</mapper>
