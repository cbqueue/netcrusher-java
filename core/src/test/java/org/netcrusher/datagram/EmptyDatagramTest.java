package org.netcrusher.datagram;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.netcrusher.core.meter.RateMeters;
import org.netcrusher.core.nio.NioUtils;
import org.netcrusher.core.reactor.NioReactor;
import org.netcrusher.datagram.bulk.DatagramBulkReflector;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.CyclicBarrier;

class EmptyDatagramTest {

    private static final InetSocketAddress CRUSHER_ADDRESS = new InetSocketAddress("127.0.0.1", 10284);

    private static final InetSocketAddress REFLECTOR_ADDRESS = new InetSocketAddress("127.0.0.1", 10285);

    private NioReactor reactor;

    private DatagramCrusher crusher;

    @BeforeEach
    void setUp() throws Exception {
        reactor = new NioReactor();

        crusher = DatagramCrusherBuilder.builder()
            .withReactor(reactor)
            .withBindAddress(CRUSHER_ADDRESS)
            .withConnectAddress(REFLECTOR_ADDRESS)
            .buildAndOpen();
    }

    @AfterEach
    void tearDown() {
        if (crusher != null) {
            crusher.close();
        }

        if (reactor != null) {
            reactor.close();
        }
    }

    @Test
    void test() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);

        DatagramBulkReflector reflector = new DatagramBulkReflector("REFLECTOR", REFLECTOR_ADDRESS, 1, barrier);
        reflector.open();

        barrier.await();
        Thread.sleep(1000);

        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(true);

        ByteBuffer bb = ByteBuffer.allocate(100);

        try {
            // sent
            bb.clear();
            bb.flip();
            int sent = channel.send(bb, CRUSHER_ADDRESS);
            Assertions.assertEquals(0, sent);

            // check
            Thread.sleep(500);

            Assertions.assertEquals(1, crusher.getClientTotalCount());

            RateMeters innerByteMeters = crusher.getInnerByteMeters();
            Assertions.assertEquals(0, innerByteMeters.getReadMeter().getTotalCount());
            Assertions.assertEquals(0, innerByteMeters.getSentMeter().getTotalCount());

            RateMeters innerPacketMeters = crusher.getInnerPacketMeters();
            Assertions.assertEquals(1, innerPacketMeters.getReadMeter().getTotalCount());
            Assertions.assertEquals(1, innerPacketMeters.getSentMeter().getTotalCount());

            // read
            bb.clear();
            InetSocketAddress address = (InetSocketAddress) channel.receive(bb);
            Assertions.assertNotNull(address);
            Assertions.assertEquals(CRUSHER_ADDRESS, address);
            Assertions.assertEquals(0, bb.position());
        } finally {
            NioUtils.close(channel);
            NioUtils.close(reflector);
        }
    }

    @Test
    void testBlockSockets() throws Exception {
        DatagramChannel channel1 = DatagramChannel.open();
        channel1.configureBlocking(true);
        // No empty datagram for connected socket
        // https://bugs.openjdk.java.net/browse/JDK-8013175
        // channel1.connect(bindAddress);

        DatagramChannel channel2 = DatagramChannel.open();
        channel2.configureBlocking(true);
        channel2.bind(REFLECTOR_ADDRESS);

        ByteBuffer bb = ByteBuffer.allocate(0);
        bb.clear();

        try {
            bb.flip();
            int sent = channel1.send(bb, REFLECTOR_ADDRESS);
            Assertions.assertEquals(0, sent);

            Thread.sleep(100);

            bb.clear();
            InetSocketAddress address = (InetSocketAddress) channel2.receive(bb);
            Assertions.assertNotNull(address);
            Assertions.assertEquals(0, bb.position());
        } finally {
            NioUtils.close(channel2);
            NioUtils.close(channel1);
        }
    }

    @Test
    void testNoCrusher() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);

        DatagramBulkReflector reflector = new DatagramBulkReflector("REFLECTOR", REFLECTOR_ADDRESS, 1, barrier);
        reflector.open();

        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(true);
        // No empty datagram for connected socket
        // https://bugs.openjdk.java.net/browse/JDK-8013175
        // channel.connect(reflectorAddress);

        barrier.await();
        Thread.sleep(1000);

        ByteBuffer bb = ByteBuffer.allocate(0);

        try {
            // sent
            bb.clear();
            bb.flip();
            int sent = channel.send(bb, REFLECTOR_ADDRESS);
            Assertions.assertEquals(0, sent);

            // read
            bb.clear();
            InetSocketAddress address = (InetSocketAddress) channel.receive(bb);
            Assertions.assertNotNull(address);
            Assertions.assertEquals(REFLECTOR_ADDRESS, address);
            Assertions.assertEquals(0, bb.position());
        } finally {
            NioUtils.close(channel);
            NioUtils.close(reflector);
        }
    }
}
