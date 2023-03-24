package org.example.chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * @author wenbo.zhang
 * @date 2022/9/22 11:15
 */
public class GroupChatServer {

    private static final int PORT = 6667;
    private Selector selector;
    private ServerSocketChannel channel;

    public GroupChatServer() {
        try {
            selector = Selector.open();
            channel = ServerSocketChannel.open();
            channel.socket().bind(new InetSocketAddress(PORT));
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        GroupChatServer server = new GroupChatServer();
        server.listen();
    }

    public void listen() {
        try {
            while (selector.select() > 0) {
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    if (key.isValid()) {

                        if (key.isAcceptable()) {
                            // 处理接入消息
                            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                            SocketChannel sc = ssc.accept();
                            sc.configureBlocking(false);
                            sc.register(selector, SelectionKey.OP_READ);
                            System.out.println("获取到一个客户端连接 : " + sc.getRemoteAddress() + " 上线!");
                        }

                        if (key.isReadable()) {
                            read(key);
                        }

                        it.remove();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void read(SelectionKey key) {
        try (SocketChannel sc = (SocketChannel) key.channel()) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            if (sc.read(buffer) > 0) {
                String msg = new String(buffer.array());
                msg = msg.replaceAll("[\t\n\r\u0000]", "");
                System.out.println("receive message from : " + sc.getRemoteAddress() + ", message: " + msg);
                multicast(msg, sc);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            offline(key);
        }
    }

    public void offline(SelectionKey key) {
        if (key != null) {
            try {
                key.cancel();
                SocketChannel sc = (SocketChannel) key.channel();
                System.out.println(sc.getRemoteAddress() + " 下线！");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void multicast(String msg, SocketChannel sc) throws IOException {
        // 服务器转发消息
        System.out.println("服务器转发消息....");
        for (SelectionKey key : selector.keys()) {
            SelectableChannel channel = key.channel();
            if (channel instanceof SocketChannel && channel != sc) {
                SocketChannel otherSc = (SocketChannel) channel;
                ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
                otherSc.write(buffer);
            }
        }
    }
}
