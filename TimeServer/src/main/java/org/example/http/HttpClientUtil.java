package org.example.http;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.Args;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author wenbo.zhangw
 * @date 2022/3/12 15:50
 */
public class HttpClientUtil {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    private static final String TIMEOUT = "timeout";

    /**
     * 长链接默认时间
     */
    private static Long keepAliveTime = 60_000L;

    /**
     * 最大连接数
     */
    private static Integer maxTotal = 1;

    /**
     * 单个路由最大连接数
     */
    private static Integer defaultMaxPerRoute = maxTotal;

    /**
     * 长链接策略
     */
    private static ConnectionKeepAliveStrategy CONNECTION_KEEP_ALIVE_STRATEGY;

    /**
     * 初始化连接池
     */
    private static PoolingHttpClientConnectionManager POOLING_HTTP_CLIENT_CONNECTION_MANAGER;

    static {
        logger.info("http client init start...");
        CONNECTION_KEEP_ALIVE_STRATEGY = getConnectionKeepAliveStrategy();
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER = init();
        logger.info("http client init success!");
    }

    public static CloseableHttpClient getHttpClient(RequestConfig requestConfig) {
        return HttpClients.custom()
                .setConnectionManager(POOLING_HTTP_CLIENT_CONNECTION_MANAGER)
                .setDefaultRequestConfig(requestConfig)
                .setKeepAliveStrategy(CONNECTION_KEEP_ALIVE_STRATEGY)
                .build();
    }

    /**
     * 请求资源或服务
     *
     * @return 返回HttpResponse对象
     */
    public static CloseableHttpResponse execute(HttpGet httpGet) throws IOException {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(5000)
                .setConnectTimeout(5000)
                .setSocketTimeout(0)
                .build();
        CloseableHttpClient httpClient = HttpClientUtil.getHttpClient(requestConfig);

        CloseableHttpResponse execute = null;
        execute = httpClient.execute(httpGet);

        return execute;
    }

    private static ConnectionKeepAliveStrategy getConnectionKeepAliveStrategy() {
        return (response, httpContext) -> {
            Args.notNull(response, "HTTP response");
            long keepAliveTime = HttpClientUtil.keepAliveTime;
            final HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                final HeaderElement he = it.nextElement();
                final String param = he.getName();
                final String value = he.getValue();
                if (value != null && TIMEOUT.equalsIgnoreCase(param)) {
                    try {
                        keepAliveTime = Long.parseLong(value) * 1000;
                    } catch (final NumberFormatException ignore) {
                    }
                }
            }
            return keepAliveTime;
        };
    }

    /**
     * 初始化pool
     */
    private static PoolingHttpClientConnectionManager init() {
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.getSocketFactory()).build());
        // 设置最大连接100
        poolingHttpClientConnectionManager.setMaxTotal(maxTotal);
        // 设置单个路由最大连接20
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
        return poolingHttpClientConnectionManager;
    }

    /**
     * 转化为字符串
     *
     * @param resp 响应对象
     * @return 返回处理结果
     */
    private static String fmt2String(HttpResponse resp) {
        String body;
        try {
            if (resp.getEntity() != null) {
                // 按指定编码转换结果实体为String类型
                body = EntityUtils.toString(resp.getEntity(), "utf-8");
            } else {//有可能是head请求
                body = resp.getStatusLine().toString();
            }
            EntityUtils.consume(resp.getEntity());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            close(resp);
        }
        return body;
    }

    /**
     * 尝试关闭response
     *
     * @param resp HttpResponse对象
     */
    private static void close(HttpResponse resp) {
        try {
            if (resp == null) {
                return;
            }
            //如果CloseableHttpResponse 是resp的父类，则支持关闭
            if (CloseableHttpResponse.class.isAssignableFrom(resp.getClass())) {
                ((CloseableHttpResponse) resp).close();
            }
        } catch (IOException e) {
            logger.error("", e);
        }
    }


}
