import io.netty.buffer.ByteBuf;
import io.netty.channel.unix.Socket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.*;
import java.lang.Comparable;

import java.util.concurrent.*;

public class GroupChatServer {
    ServerSocketChannel ssc;
    Selector selector;
    int PORT = 8000;
    ByteBuffer recv = ByteBuffer.allocate(1024);


    public GroupChatServer() throws IOException {
        ssc = ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress(PORT));
        ssc.configureBlocking(false);

        selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void listen() throws IOException {
        while(true) {
            int nReady = selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> it = selectionKeys.iterator();
            while(it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();

                if(key.isAcceptable()) {
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    sc.register(selector, SelectionKey.OP_READ);
                    System.out.println(sc.getRemoteAddress() + " 上线 ");
                }
                if(key.isReadable()) {
                    SocketChannel sc = (SocketChannel) key.channel();
                    byte[] info = readInfo(sc, key);
                    if(info != null) {
                        broadcast(info, sc);
                    }
                }
            }

        }
    }

    private byte[] readInfo(SocketChannel sc, SelectionKey key) throws IOException {
        int nRead = 1;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write((sc.getRemoteAddress() + "说：").getBytes("utf-8"));
        recv.clear();
        while (nRead > 0) {
            try {
                nRead = sc.read(recv);
            } catch (IOException e) {
                System.out.println(sc.getRemoteAddress() + "下线");
                key.cancel();
                sc.close();
                return null;
            }
            baos.write(Arrays.copyOf(recv.array(), recv.position()));
            recv.rewind();
        }
        return baos.toByteArray();
    }

    private void broadcast(byte[] info, Channel self) throws IOException {
        System.out.println("转发：“" + new String(info) + "”");
        ByteBuffer buf = ByteBuffer.wrap(info);
        for(SelectionKey key: selector.keys()) {
            Channel c = key.channel();
            if(c instanceof SocketChannel && c != self) {
                SocketChannel sc = (SocketChannel) c;
                System.out.println("给" + sc.getRemoteAddress() + "转发");
                sc.write(buf);
                buf.rewind();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        GroupChatServer s = new GroupChatServer();
        s.listen();
    }



//    public static void main(String[] args) throws Exception {
//        try {
//            ServerSocketChannel ssc = ServerSocketChannel.open();
//            ssc.socket().bind(new InetSocketAddress("127.0.0.1", 8000));
//            ssc.configureBlocking(false);
//            Selector selector = Selector.open();
//            // 注册 channel，并且指定感兴趣的事件是 Accept
//            ssc.register(selector, SelectionKey.OP_ACCEPT);
//
//            ByteBuffer readBuff = ByteBuffer.allocate(1024);
//            ByteBuffer writeBuff = ByteBuffer.allocate(128);
//            writeBuff.put("received\r\n".getBytes());
//            writeBuff.flip();
//
//            while (true) {
//                int nReady = selector.select();
//                System.out.printf("select完成, nReady: %d\n", nReady);
//                Set<SelectionKey> keys = selector.selectedKeys();
//                Iterator<SelectionKey> it = keys.iterator();
//
//                while (it.hasNext()) {
//                    SelectionKey key = it.next();
//                    it.remove();
//
//                    if (key.isAcceptable()) {
//                        // 创建新的连接，并且把连接注册到selector上，而且，
//                        // 声明这个channel只对读操作感兴趣。
//                        System.out.println("触发accept");
//                        SocketChannel socketChannel = ssc.accept();
//                        socketChannel.configureBlocking(false);
//                        socketChannel.register(selector, SelectionKey.OP_READ);
//                    }
//                    if (key.isReadable()) {
//                        System.out.println("触发read");
//                        SocketChannel socketChannel = (SocketChannel) key.channel();
//                        readBuff.clear();
//                        socketChannel.read(readBuff);
//
//                        readBuff.flip();
//                        System.out.println("received : " + new String(readBuff.array()));
//                        key.interestOps(SelectionKey.OP_WRITE);
//                    }
//                    else if (key.isWritable()) {
//                        System.out.println("触发write");
//                        writeBuff.rewind();
//                        SocketChannel socketChannel = (SocketChannel) key.channel();
//                        socketChannel.write(writeBuff);
//
//                        key.interestOps(SelectionKey.OP_READ);
//                    }
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}