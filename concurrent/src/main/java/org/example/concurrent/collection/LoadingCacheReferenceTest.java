package org.example.concurrent.collection;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * @author wenbo.zhangw
 * @date 2022/5/13 19:08
 */
public class LoadingCacheReferenceTest {

    private static final ExecutorService executorService = new ThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors(), 60L, TimeUnit.SECONDS, new SynchronousQueue(), (r) -> {
        Thread thread = new Thread(r);
        thread.setName("BASE_LOADING_CACHE");
        return thread;
    }, new ThreadPoolExecutor.CallerRunsPolicy());

    private static final Logger logger = LoggerFactory.getLogger(LoadingCacheReferenceTest.class);

    public static void main(String[] args) throws ExecutionException {
        logger.info("start");
        LoadingCache<Object, Object> cache = CacheBuilder.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .refreshAfterWrite(1, TimeUnit.HOURS)
                .softValues()
                .removalListener(notification -> logger.info("listener for key {} is removed! cause: {}", notification.getKey(), notification.getCause()))
                .build(new CacheLoader<Object, Object>() {
                    @Override
                    public Object load(Object key) throws Exception {
                        logger.info("Loading key {}", key);
                        // 1m
                        return new byte[1024 * 1024];
                    }

                    @Override
                    public ListenableFuture<Object> reload(Object key, Object oldValue) throws Exception {
                        logger.info("Reloading key {}", key);
                        ListenableFutureTask<Object> futureTask = ListenableFutureTask.create(() -> {
                            return load(key);
                        });
                        executorService.execute(futureTask);
                        return futureTask;
                    }
                });

        cache.get("key1");
        cache.get("key2");
        cache.get("key3");
    }
}
