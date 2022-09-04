package org.example.timeserver.aio.client;

import java.io.IOException;

/**
 * @author wenbo.zhangw
 * @date 2022/6/9 15:51
 */
public class TimeClient {

    private static int SERVER_PORT = 8080;

    public static void main(String[] args) throws IOException {
        if (args != null && args.length > 0) {
            SERVER_PORT = Integer.parseInt(args[0]);
        }
        new Thread(new AsyncTimeClientHandler("127.0.0.1", SERVER_PORT), "AIO-AsyncTimerClientHandler-1").start();
    }
}
