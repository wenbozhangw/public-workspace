package org.example.nio;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author wenbo.zhang
 * @date 2022/9/21 18:38
 */
public class TimeServer {

    private static final Integer DEFAULT_SERVER_PORT = 8080;

    private static final ExecutorService pool = Executors.newFixedThreadPool(1);

    public static void main(String[] args) {
        int port = DEFAULT_SERVER_PORT;
        if (args != null && args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        pool.submit(new MultiplexerTimeServer(port));
    }
}
