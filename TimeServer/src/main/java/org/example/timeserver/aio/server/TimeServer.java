package org.example.timeserver.aio.server;

/**
 * @author wenbo.zhangw
 * @date 2022/6/6 16:51
 */
public class TimeServer {

    private static int SERVER_PORT = 8080;

    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            SERVER_PORT = Integer.parseInt(args[0]);
        }
        AsyncTimeServerHandler timeServer = new AsyncTimeServerHandler(SERVER_PORT);
        new Thread(timeServer, "AIO-AsyncTimeServerHandler-1").start();
    }
}
