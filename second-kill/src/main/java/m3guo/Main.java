package m3guo;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Main {

    private static final AtomicLong counter = new AtomicLong(0);


    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            100,
            300,
            1,
            TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("[Kill-M3Guo-" + counter.getAndIncrement() + "]");
                    thread.setDaemon(true);
                    return thread;
                }
            }, new ThreadPoolExecutor.CallerRunsPolicy());

    private static final String JiXianKill = "https://www.m3guo.com/activity/202107/julhuigui/ajax/GiftHander.ashx?action=bonus&exno=2";

    private static final String MoneyKill = "https://www.m3guo.com/activity/202107/julhuigui/ajax/GiftHander.ashx?action=bonus&alipaycount=13525382051&alipayname=张文博&alipaytel=13525382051&identityno=410482199809167731&exno=1";

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {

        OkHttpClient httpClient = new OkHttpClient();
        String cookie = "" +
                "UM_distinctid=179bc708679108-0db84c0c510c0e-4c3f237d-fff00-179bc70867a564; CNZZDATA1212180=cnzz_eid%3D343096510-1622359803-https%253A%252F%252Fwww.m3guo.com%252F%26ntime%3D1625971995; ASP.NET_SessionId=3tmwdxm4w14yjp55uvzrjn55" +
                "";
        Headers headers = new Headers.Builder()
                .add("Connection", "keep-alive")
                .add("Cookie", cookie)
                .add("Host", "www.m3guo.com")
                .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:88.0) Gecko/20100101 Firefox/88.0")
                .build();
        Request request = new Request.Builder()
                .url(MoneyKill)
                .get()
                .headers(headers)
                .build();

        while (true){
            executor.submit(() -> {
                try {
                    Call call = httpClient.newCall(request);
                    Response response = call.execute();
                    logger.info("{} success, response {}", Thread.currentThread().getName(), response.body().string());
                } catch (Exception e) {
                    logger.error("execute fail, ", e);
                }
            });
        }
    }

}
