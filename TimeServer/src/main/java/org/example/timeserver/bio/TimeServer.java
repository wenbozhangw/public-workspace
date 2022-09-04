package org.example.timeserver.bio;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * @author wenbo.zhangw
 * @date 2022/6/6 16:51
 */
public class TimeServer {

    private static final ExecutorService executorService = new ThreadPoolExecutor(0, 10, 1, TimeUnit.MINUTES, new LinkedBlockingDeque<>(100),
            new ThreadFactory() {
                private int counter = 0;

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("receiver-pool-" + counter++);
                    thread.setUncaughtExceptionHandler((t, e) -> e.printStackTrace());
                    return thread;
                }
            }, new ThreadPoolExecutor.CallerRunsPolicy());
    private static int SERVER_PORT = 8080;

    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            SERVER_PORT = Integer.parseInt(args[0]);
        }
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("The time server is start in port: " + SERVER_PORT);
            Socket socket = null;
            while (true) {
                socket = serverSocket.accept();
                executorService.submit(new TimeServerHandler(socket));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                    serverSocket = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
