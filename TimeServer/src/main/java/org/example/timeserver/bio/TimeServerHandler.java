package org.example.timeserver.bio;

import java.io.BufferedReader;
import java.net.Socket;
import java.util.Date;

/**
 * @author wenbo.zhangw
 * @date 2022/6/6 16:54
 */
public class TimeServerHandler implements Runnable {

    private final Socket socket;

    public TimeServerHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader br = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
             java.io.PrintWriter pw = new java.io.PrintWriter(socket.getOutputStream(), true)) {
            String body = null;
            String currentTime = null;
            while (true) {
                body = br.readLine();
                if (body == null) {
                    break;
                }
                System.out.println("The time server receive order: " + body);
                currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date(
                        System.currentTimeMillis()
                ).toString() : "BAD ORDER";

                pw.println(currentTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
