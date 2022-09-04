package org.example.http;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * @author wenbo.zhangw
 * @date 2022/6/6 17:41
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 60,
            java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<>(10), new ThreadPoolExecutor.CallerRunsPolicy());

    public static void main(String[] args) throws IOException, InterruptedException {

        for (int i = 0; i < 10; i++) {
            HttpGet httpGet = new HttpGet("http://10.58.12.66:10888/ok");
            CloseableHttpResponse response = null;
            Future<CloseableHttpResponse> future = null;
            try {
                future = executor.submit(() -> {
                    try {
                        return HttpClientUtil.execute(httpGet);
                    } catch (Exception e) {
                        logger.error("", e);
                        return null;
                    } finally {
                        httpGet.abort();
                        httpGet.releaseConnection();
                    }
                });
                response = future.get(5, TimeUnit.SECONDS);
                System.out.println("response = " + response);
            } catch (Exception e) {
                if (e instanceof TimeoutException && future != null) {
                    logger.info(Thread.currentThread().getName() + " start cancel future");
                    logger.error("", e);
                    boolean cancel = future.cancel(true);
                    if (!cancel) {
                        System.out.println(Thread.currentThread().getName() + ": cancel failed");
                    }
                }
            } finally {
                if (null != response) {
                    EntityUtils.consume(response.getEntity());
                }
                
            }
        }
    }
}
