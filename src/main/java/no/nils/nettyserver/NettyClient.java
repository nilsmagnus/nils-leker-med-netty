package no.nils.nettyserver;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.*;
import java.util.stream.IntStream;

public class NettyClient {

    private final Bootstrap bootstrap;

    public NettyClient(Executor executor) throws InterruptedException {
        final EventLoopGroup workerGroup = new NioEventLoopGroup(10, executor);

        bootstrap = new Bootstrap(); // (1)
        bootstrap.group(workerGroup); // (2)
        bootstrap.channel(NioSocketChannel.class); // (3)
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true); // (4)
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast("saymyname", new ChannelHandlerAdapter() {
                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                        super.exceptionCaught(ctx, cause);
                        cause.printStackTrace();
                    }

                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        final ByteBuf time = ctx.alloc().buffer("hello".getBytes().length); // (2)
                        time.writeBytes("hello".getBytes());
                        ctx.writeAndFlush(time);
                    }
                });
            }
        });
    }

    public Runnable runnableSender(int port, String host, int times, BlockingQueue<Boolean> doneChannel) throws InterruptedException {
        System.out.println(String.format("Send %d messages now.", times));
        final CountDownLatch con = new CountDownLatch(times);

        return () -> {
            IntStream.range(0, times).forEach(i -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                send(port, host);
                System.out.println("sent " + i);
                con.countDown();
            });
            try {
                con.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            doneChannel.offer(Boolean.TRUE);
        };
    }

    public void send(int port, String host) {
        try {
            // Start the client.
            ChannelFuture channelFuture = bootstrap.connect(host, port); // (5)
            channelFuture.channel().close();
            // Wait until the connection is closed.
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }


}
