package me.zhengjie.utils;

import org.slf4j.MDC;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ThreadPoolUtil {

    /**
     * 核心线程数
     */
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 最大线程数
     */
    private static final int MAX_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 4;

    /**
     * 队列容量
     */
    private static final int QUEUE_CAPACITY = 1000;

    /**
     * 空闲线程存活时间
     */
    private static final int KEEP_ALIVE_SECONDS = 60;

    /**
     * 线程编号
     */
    private static final AtomicInteger THREAD_NUM = new AtomicInteger(1);

    /**
     * 线程池实例（单例）
     */
    private static final ThreadPoolExecutor EXECUTOR =
            new ThreadPoolExecutor(
                    CORE_POOL_SIZE,
                    MAX_POOL_SIZE,
                    KEEP_ALIVE_SECONDS,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                    r -> new Thread(r, "biz-thread-" + THREAD_NUM.getAndIncrement()),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

    /**
     * 提交 Runnable 任务
     */
    public static void execute(Runnable task) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        EXECUTOR.execute(() -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                task.run();
            } finally {
                MDC.clear();
            }
        });
    }

    /**
     * 提交 Callable 任务
     */
    public static <T> Future<T> submit(Callable<T> task) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return EXECUTOR.submit(() -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                return task.call();
            } finally {
                MDC.clear();
            }
        });
    }

    public static Future<?> submit(Runnable task) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return EXECUTOR.submit(() -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                task.run();
            } finally {
                MDC.clear();
            }
        });
    }

    public static <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        List<Callable<T>> wrappedTasks = tasks.stream().map(task -> (Callable<T>) () -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                return task.call();
            } finally {
                MDC.clear();
            }
        }).collect(Collectors.toList());
        return EXECUTOR.invokeAll(wrappedTasks);
    }

    /**
     * 获取线程池状态（便于监控）
     */
    public static String stats() {
        return String.format(
                "poolSize=%d, active=%d, queued=%d, completed=%d",
                EXECUTOR.getPoolSize(),
                EXECUTOR.getActiveCount(),
                EXECUTOR.getQueue().size(),
                EXECUTOR.getCompletedTaskCount()
        );
    }

}
