package me.zhengjie.utils;

import cn.hutool.core.util.StrUtil;
import me.zhengjie.exception.BadRequestException;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class GeneratorDataSourceSupport {

    public static final String MASTER = "master";
    public static final String INVESTMENT_STRATEGY = "investment_strategy";

    private static final Set<String> ALLOWED = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(MASTER, INVESTMENT_STRATEGY)));

    private GeneratorDataSourceSupport() {
    }

    public static String normalize(String dataSource) {
        String normalized = StrUtil.blankToDefault(dataSource, MASTER);
        if (!ALLOWED.contains(normalized)) {
            throw new BadRequestException("不支持的数据源：" + normalized);
        }
        return normalized;
    }

    public static Set<String> allowed() {
        return ALLOWED;
    }
}
