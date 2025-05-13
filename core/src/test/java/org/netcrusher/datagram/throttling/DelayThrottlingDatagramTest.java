package org.netcrusher.datagram.throttling;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.netcrusher.core.nio.NioUtils;
import org.netcrusher.core.reactor.NioReactor;
import org.netcrusher.core.throttle.DelayThrottler;
import org.netcrusher.datagram.DatagramCrusher;
import org.netcrusher.datagram.DatagramCrusherBuilder;
import org.netcrusher.datagram.bulk.DatagramBulkClient;
import org.netcrusher.datagram.bulk.DatagramBulkReflector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

class DelayThrottlingDatagramTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelayThrottlingDatagramTest.class);

    private static final int CLIENT_PORT = 10182;

    private static final int CRUSHER_PORT = 10183;

    private static final int REFLECTOR_PORT = 10184;

    private static final String HOSTNAME = "127.0.0.1";

    private static final long COUNT = 1_000;

    private static final long SEND_WAIT_MS = 20_000;

    private static final long READ_WAIT_MS = 10_000;

    private NioReactor reactor;

    private DatagramCrusher crusher;

    @BeforeEach
    void setUp() throws Exception {
        reactor = new NioReactor(10);

        crusher = DatagramCrusherBuilder.builder()
            .withReactor(reactor)
            .withBindAddress(HOSTNAME, CRUSHER_PORT)
            .withConnectAddress(HOSTNAME, REFLECTOR_PORT)
            .withIncomingGlobalThrottler(new DelayThrottler(500, TimeUnit.MILLISECONDS))
            .withOutgoingThrottlerFactory(addr -> new DelayThrottler(500, TimeUnit.MILLISECONDS))
            .withCreationListener(addr -> LOGGER.info("Client is created <{}>", addr))
            .withDeletionListener((addr, byteMeters, packetMeters) -> LOGGER.info("Client is deleted <{}>", addr))
            .buildAndOpen();
    }

    @AfterEach
    void tearDown() {
        if (crusher != null) {
            crusher.close();
            Assertions.assertFalse(crusher.isOpen());
        }

        if (reactor != null) {
            reactor.close();
            Assertions.assertFalse(reactor.isOpen());
        }
    }

    @Test
    void test() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(3);

        DatagramBulkClient client = new DatagramBulkClient("CLIENT_DELAYED",
            new InetSocketAddress(HOSTNAME, CLIENT_PORT),
            new InetSocketAddress(HOSTNAME, CRUSHER_PORT),
            COUNT,
            barrier,
            barrier);

        DatagramBulkReflector reflector = new DatagramBulkReflector("REFLECTOR",
            new InetSocketAddress(HOSTNAME, REFLECTOR_PORT),
            COUNT,
            barrier);

        reflector.open();
        try {
            client.open();
            try {
                final byte[] producerDigest = client.awaitProducerResult(SEND_WAIT_MS).getDigest();
                final byte[] consumerDigest = client.awaitConsumerResult(READ_WAIT_MS).getDigest();
                final byte[] reflectorDigest = reflector.awaitReflectorResult(READ_WAIT_MS).getDigest();

                Assertions.assertArrayEquals(producerDigest, consumerDigest);
                Assertions.assertArrayEquals(producerDigest, reflectorDigest);
            } finally {
                NioUtils.close(client);
            }
        } finally {
            NioUtils.close(reflector);
        }
    }
}
