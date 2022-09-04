package org.example.timeserver.nio.server;

import java.io.IOException;

/**
 * @author wenbo.zhangw
 * @date 2022/6/9 14:09
 */
public class TimeServer {

    private static int SERVER_PORT = 8080;

    public static void main(String[] args) throws IOException {
        if (args != null && args.length > 0) {
            SERVER_PORT = Integer.parseInt(args[0]);
        }
        MultiplexerTimeServer timeServer = new MultiplexerTimeServer(SERVER_PORT);
        new Thread(timeServer, "NIO-MultiplexerTimeServer-1").start();
    }
}
