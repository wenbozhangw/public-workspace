package org.example.utils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadPoolUtils {

    /**
     * 核心线程数
     */
    private static int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 线程池最大线程数
     */
    private static int MAX_POOL_SIZE = CORE_POOL_SIZE * 4;

    /**
     * 额外线程空状态生存时间
     */
    private static int KEEP_ALIVE_TIME = 30;

    /**
     * 阻塞队列。当核心线程都被占用，且阻塞队列已满的情况下，才会开启额外线程。
     */
    private static BlockingQueue<Runnable> workQueue = new LinkedBlockingDeque<>(200);

    /**
     * 线程池对拒绝任务的处理策略
     **/
    private static RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();

    /**
     * 线程池
     */
    public static final ThreadPoolExecutor threadPool;

    private ThreadPoolUtils() {}

    /**
     * 线程工厂
     **/
    private static ThreadFactory threadFactory = new ThreadFactory() {
        private final AtomicLong atomicLong = new AtomicLong();

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "ThreadPoolUtils-" + atomicLong.getAndIncrement());
        }
    };

    static {
        threadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, workQueue,
                threadFactory, handler);
    }

    public static void execute(Runnable runnable) {
        threadPool.execute(runnable);
    }

    public static Future submit(Runnable runnable) {
        return threadPool.submit(runnable);
    }

    public static void execute(FutureTask futureTask) {
        threadPool.execute(futureTask);
    }


}
