package org.netcrusher.datagram.throttling;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.netcrusher.core.nio.NioUtils;
import org.netcrusher.core.reactor.NioReactor;
import org.netcrusher.datagram.DatagramCrusher;
import org.netcrusher.datagram.bulk.DatagramBulkClient;
import org.netcrusher.datagram.bulk.DatagramBulkReflector;
import org.netcrusher.datagram.bulk.DatagramBulkResult;

import java.net.InetSocketAddress;
import java.util.concurrent.CyclicBarrier;

public abstract class AbstractRateThrottlingDatagramTest {

    private static final int CLIENT_PORT = 10182;

    private static final int CRUSHER_PORT = 10183;

    private static final int REFLECTOR_PORT = 10184;

    private static final String HOSTNAME = "127.0.0.1";

    private static final long COUNT = 1_000;

    private static final long SEND_WAIT_MS = 30_000;

    private static final long READ_WAIT_MS = 100;

    private static final double RATE_PRECISION = 0.05;

    private NioReactor reactor;

    private DatagramCrusher crusher;

    @BeforeEach
    public void setUp() throws Exception {
        reactor = new NioReactor(5);

        crusher = createCrusher(reactor, HOSTNAME, CRUSHER_PORT, REFLECTOR_PORT);
    }

    @AfterEach
    public void tearDown() throws Exception {
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
    public void test() throws Exception {
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

        DatagramBulkResult producerResult;
        try {
            producerResult = client.awaitProducerResult(SEND_WAIT_MS);
        } finally {
            NioUtils.close(client);
            NioUtils.close(reflector);
        }

        DatagramBulkResult consumerResult = client.awaitConsumerResult(READ_WAIT_MS);
        DatagramBulkResult reflectorResult = reflector.awaitReflectorResult(READ_WAIT_MS);

        verify(producerResult, consumerResult, reflectorResult, RATE_PRECISION);
    }

    protected abstract DatagramCrusher createCrusher(NioReactor reactor, String host, int bindPort, int connectPort);

    protected abstract void verify(
        DatagramBulkResult producerResult,
        DatagramBulkResult consumerResult,
        DatagramBulkResult reflectorResult,
        double precisionAllowed);

}
