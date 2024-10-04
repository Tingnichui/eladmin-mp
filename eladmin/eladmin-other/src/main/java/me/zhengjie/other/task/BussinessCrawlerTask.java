package me.zhengjie.other.task;

import cn.hutool.crypto.SecureUtil;

import java.util.HashMap;

public class BussinessCrawlerTask {

    public static void main(String[] args) {
        xianyuWeb();
    }

    private static void xianyuWeb() {
        String token = "c9e2b2811136c903e90db9ae3d3";
        String time = Long.toString(System.currentTimeMillis());
        String appKey = "12574478";
        String data = "{\"pageNumber\":1,\"keyword\":\"小米8\",\"fromFilter\":false,\"rowsPerPage\":30,\"sortValue\":\"\",\"sortField\":\"\",\"customDistance\":\"\",\"gps\":\"\",\"propValueStr\":{},\"customGps\":\"\"}";

        String md5Str = String.format("%s&%s&%s&%s", token, time, appKey, data);
        String s = SecureUtil.md5(md5Str);
        System.err.println(md5Str);
        System.err.println(time);
        System.err.println(s);
    }
}
