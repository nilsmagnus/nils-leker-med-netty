package no.nils.nettyserver;

import org.junit.Test;

import java.util.concurrent.*;

public class NettyServerLoadTest {

    final int localPort = 8082;
    final String hostname = "localhost";
    final int messageCount = 1000;

    @Test
    public void test_1000_messages() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(messageCount);

        ExecutorService tpe = Executors.newCachedThreadPool();
        LinkedBlockingQueue<byte[]> consumerQueue = new LinkedBlockingQueue<>();
        new NettyServer(tpe, consumerQueue::offer).start(localPort);

        Thread.sleep(200); // give server a small break
        BlockingQueue<Boolean> doneChannel = new LinkedBlockingQueue<>();
        tpe.submit(new NettyClient(tpe).runnableSender(localPort, hostname, messageCount, doneChannel));

        tpe.submit(new Thread(() -> {
            try {
                while (true) {

                    byte[] buffer = consumerQueue.take();
                    System.out.println(String.format("Got message of size %d", buffer.length));
                    countDownLatch.countDown();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));

        System.out.println("Awaiting latch");
        boolean allMessagesReceived = countDownLatch.await(30, TimeUnit.SECONDS);

        System.out.println("latches left:" + countDownLatch.getCount());
        assert allMessagesReceived;

        tpe.shutdown();
    }
}
