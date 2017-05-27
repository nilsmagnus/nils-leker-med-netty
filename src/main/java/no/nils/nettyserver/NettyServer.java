package no.nils.nettyserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class NettyServer {

    private final ServerBootstrap serverBootstrap = new ServerBootstrap();

    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<byte[]> datachannel = new LinkedBlockingQueue<>();
        new NettyServer(Executors.newCachedThreadPool(), datachannel::offer).start(8082);
        while(true){
            byte[] data = datachannel.take();
            System.out.println("data received: " + new String(data));
        }
    }

    public NettyServer(ExecutorService tpe, Consumer<byte[]> consumer) {
        NioEventLoopGroup acceptorGroup = new NioEventLoopGroup(1, tpe); // 2 threads
        NioEventLoopGroup handlerGroup = new NioEventLoopGroup(1, tpe); // 10 threads

        serverBootstrap.group(acceptorGroup, handlerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new SocketInitializer(consumer))
                .option(ChannelOption.SO_BACKLOG, 5)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

    }

    public NettyServer start(int port) throws InterruptedException {
        serverBootstrap.localAddress(port).bind().sync();
        return this;
    }

    private class SocketInitializer extends ChannelInitializer<SocketChannel> {

        private final Consumer<byte[]> consumer;

        public SocketInitializer(Consumer<byte[]> consumer) {
            this.consumer = consumer;
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();

            pipeline.addLast("Read from socket", new ChannelHandlerAdapter() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    ByteBuf buffer = (ByteBuf) msg;
                    byte[] destination = new byte[buffer.readableBytes()];
                    buffer.readBytes(destination);
                    consumer.accept(destination);

                    buffer.release();
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                    cause.printStackTrace();
                    ctx.close();
                }
            });
        }
    }
}
