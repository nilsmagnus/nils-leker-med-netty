package no.nils.nettyserver;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.stream.IntStream;

/// Plain socket client for testing, nothing to do with nio or netty
public class SocketClient {

    public final int port;
    public final String host;

    public SocketClient(int port, String host) {
        this.port = port;
        this.host = host;
    }

    public Runnable sendInNewThread(byte[] message, int times) {
        return new Thread(() -> IntStream.range(0, times).forEach(i -> {
            send(message);
            System.out.println(i);
        }));
    }

    public void send(byte[] message) {
        new Thread(() -> {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(host, port));
                socket.getOutputStream().write(message);
                socket.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}
