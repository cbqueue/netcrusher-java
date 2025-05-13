package org.netcrusher.datagram;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.netcrusher.core.filter.InverseFilter;
import org.netcrusher.core.filter.PassFilter;
import org.netcrusher.core.filter.TransformFilter;
import org.netcrusher.core.filter.TransformFilters;
import org.netcrusher.core.meter.RateMeters;
import org.netcrusher.core.nio.NioUtils;
import org.netcrusher.core.reactor.NioReactor;
import org.netcrusher.core.throttle.Throttler;
import org.netcrusher.datagram.bulk.DatagramBulkClient;
import org.netcrusher.datagram.bulk.DatagramBulkReflector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CyclicBarrier;

class BulkDatagramTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkDatagramTest.class);

    private static final int CLIENT_PORT = 10182;

    private static final int CRUSHER_PORT = 10183;

    private static final int REFLECTOR_PORT = 10184;

    private static final String HOSTNAME = "127.0.0.1";

    private static final long COUNT = 2_000;

    private static final long SEND_WAIT_MS = 120_000;

    private static final long READ_WAIT_MS = 30_000;

    private NioReactor reactor;

    private DatagramCrusher crusher;

    @BeforeEach
    void setUp() throws Exception {
        reactor = new NioReactor();

        crusher = DatagramCrusherBuilder.builder()
            .withReactor(reactor)
            .withBindAddress(HOSTNAME, CRUSHER_PORT)
            .withConnectAddress(HOSTNAME, REFLECTOR_PORT)
            .withIncomingTransformFilterFactory(
                TransformFilters.all(addr -> TransformFilter.NOOP, (addr) -> InverseFilter.INSTANCE))
            .withOutgoingTransformFilterFactory(
                TransformFilters.all(addr -> InverseFilter.INSTANCE, (addr) -> TransformFilter.NOOP))
            .withIncomingPassFilterFactory(addr -> PassFilter.NOOP)
            .withOutgoingPassFilterFactory(addr -> PassFilter.NOOP)
            .withIncomingGlobalThrottler(Throttler.NOOP)
            .withOutgoingThrottlerFactory(addr -> Throttler.NOOP)
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
        crusher.freeze();
        Assertions.assertTrue(crusher.isFrozen());
        Assertions.assertTrue(crusher.isOpen());

        crusher.unfreeze();
        Assertions.assertFalse(crusher.isFrozen());
        Assertions.assertTrue(crusher.isOpen());

        CyclicBarrier barrier = new CyclicBarrier(3);

        DatagramBulkClient client = new DatagramBulkClient("CLIENT",
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
        client.open();

        try {
            final byte[] producerDigest = client.awaitProducerResult(SEND_WAIT_MS).getDigest();
            final byte[] consumerDigest = client.awaitConsumerResult(READ_WAIT_MS).getDigest();

            reflector.awaitReflectorResult(READ_WAIT_MS).getDigest();

            Assertions.assertEquals(1, crusher.getClientAddresses().size());
            InetSocketAddress clientAddress = crusher.getClientAddresses().iterator().next();
            Assertions.assertNotNull(clientAddress);

            RateMeters innerByteMeters = crusher.getInnerByteMeters();
            Assertions.assertTrue(innerByteMeters.getReadMeter().getTotalCount() > 0);
            Assertions.assertTrue(innerByteMeters.getSentMeter().getTotalCount() > 0);

            RateMeters outerByteMeters = crusher.getClientByteMeters(clientAddress);
            Assertions.assertTrue(outerByteMeters.getReadMeter().getTotalCount() > 0);
            Assertions.assertTrue(outerByteMeters.getSentMeter().getTotalCount() > 0);

            RateMeters innerPacketMeters = crusher.getInnerPacketMeters();
            Assertions.assertEquals(COUNT, innerPacketMeters.getReadMeter().getTotalCount());
            Assertions.assertEquals(COUNT, innerPacketMeters.getSentMeter().getTotalCount());

            RateMeters outerPacketMeters = crusher.getClientPacketMeters(clientAddress);
            Assertions.assertEquals(COUNT, outerPacketMeters.getReadMeter().getTotalCount());
            Assertions.assertEquals(COUNT, outerPacketMeters.getSentMeter().getTotalCount());

            Assertions.assertArrayEquals(producerDigest, consumerDigest);
        } finally {
            NioUtils.close(client);
            NioUtils.close(reflector);
        }
    }
}
