import io.netty.channel.unix.Socket;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.nio.channels.*;
import java.nio.*;

import java.net.*;
import java.net.UnknownHostException.*;
import java.net.InetAddress;

import java.util.*;

public class GroupChatClient {
    SocketChannel sc;
    Selector selector;
    String IP = "127.0.0.1";
    int PORT = 8000;
    Thread readThread;

    public GroupChatClient() throws IOException {
        sc = SocketChannel.open();
        sc.connect(new InetSocketAddress(IP, PORT));
        sc.configureBlocking(false);
        selector = Selector.open();
        sc.register(selector, SelectionKey.OP_READ);

        readThread = new Thread() {
            Selector readSelector = selector;
            SocketChannel readSc = sc;
            ByteBuffer recv = ByteBuffer.allocate(1024);
            @Override
            public void run() {
                System.out.println("开始监听服务端");
                while(true) {
                    try {
                        int nReady = readSelector.select();
                        Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                        Iterator<SelectionKey> it = selectionKeys.iterator();
                        while(it.hasNext()) {
                            SelectionKey key = it.next();
                            it.remove();

                            if(key.isReadable()) {
                                SocketChannel sc = (SocketChannel) key.channel();
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                int nRead = 1;
                                recv.clear();
                                while(nRead > 0) {
                                    //System.out.println("开始读取");
                                    nRead = sc.read(recv);
                                    //System.out.println("读取成功:" + new String(recv.array()));
                                    baos.write(Arrays.copyOf(recv.array(), recv.position()));
                                    recv.rewind();
                                }
                                byte[] info = baos.toByteArray();
                                System.out.println(new String(info, "utf-8"));
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("服务器关闭或本地读写错误");
                        e.printStackTrace();
                        System.exit(0);
                    }

                }
            }
        };
        readThread.start();
    }

    public void speak(InputStream in) throws IOException {
        Scanner scanner = new Scanner(in);
        while(scanner.hasNextLine()) {
            byte[] info = scanner.nextLine().getBytes("utf-8");
            sc.write(ByteBuffer.wrap(info));
        }
    }

    public static void main(String[] args) throws IOException {
        GroupChatClient c = new GroupChatClient();
        c.speak(System.in);

    }


//    public static void main(String[] args) {
//        SocketChannel socketChannel = null;
//        try {
//            socketChannel = SocketChannel.open();
//            socketChannel.connect(new InetSocketAddress("127.0.0.1", 8000));
//
//            ByteBuffer writeBuffer = ByteBuffer.allocate(128);
//            Scanner in = new Scanner(System.in);
//            while(true) {
////                writeBuffer.clear();
////                String s = in.nextLine();
////                writeBuffer.put(s.getBytes());
////
////                writeBuffer.flip();
////                socketChannel.write(writeBuffer);
//
//                socketChannel.read(writeBuffer);
//                System.out.println(new String(writeBuffer.array()));
//
//            }
////            socketChannel.read(writeBuffer);
////            System.out.println(writeBuffer.array());
////            socketChannel.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}