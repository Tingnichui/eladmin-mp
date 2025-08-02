package me.zhengjie.invest.util;

import me.zhengjie.invest.domain.BinanceAccountInfo;

public class BinanceAccountContextHolder {

    private static final ThreadLocal<BinanceAccountInfo> CONTEXT = new ThreadLocal<>();

    public static void set(BinanceAccountInfo info) {
        CONTEXT.set(info);
    }

    public static BinanceAccountInfo get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static void runWith(BinanceAccountInfo info, Runnable runnable) {
        try {
            set(info);
            runnable.run();
        } finally {
            clear();
        }
    }

}
