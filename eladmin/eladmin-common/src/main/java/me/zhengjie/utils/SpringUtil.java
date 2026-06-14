package me.zhengjie.utils;

import java.util.Arrays;

public class SpringUtil extends cn.hutool.extra.spring.SpringUtil {

    public static boolean isDevEnv() {
        String[] activeProfiles = SpringUtil.getActiveProfiles();
        return Arrays.stream(activeProfiles).allMatch(v -> v.equals("dev"));
    }

}
