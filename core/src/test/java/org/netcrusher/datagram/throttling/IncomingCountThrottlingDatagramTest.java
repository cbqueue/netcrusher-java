package org.netcrusher.datagram.throttling;

import org.junit.jupiter.api.Assertions;
import org.netcrusher.core.reactor.NioReactor;
import org.netcrusher.core.throttle.rate.PacketRateThrottler;
import org.netcrusher.datagram.DatagramCrusher;
import org.netcrusher.datagram.DatagramCrusherBuilder;
import org.netcrusher.datagram.bulk.DatagramBulkResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class IncomingCountThrottlingDatagramTest extends AbstractRateThrottlingDatagramTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncomingCountThrottlingDatagramTest.class);

    private static final int PACKET_PER_SEC = 80;

    @Override
    protected DatagramCrusher createCrusher(NioReactor reactor, String host, int bindPort, int connectPort) {
        return DatagramCrusherBuilder.builder()
            .withReactor(reactor)
            .withBindAddress(host, bindPort)
            .withConnectAddress(host, connectPort)
            .withIncomingGlobalThrottler(new PacketRateThrottler(PACKET_PER_SEC, 1, TimeUnit.SECONDS))
            .withCreationListener(addr -> LOGGER.info("Client is created <{}>", addr))
            .withDeletionListener((addr, byteMeters, packetMeters) -> LOGGER.info("Client is deleted <{}>", addr))
            .buildAndOpen();
    }

    @Override
    protected void verify(
        DatagramBulkResult producerResult,
        DatagramBulkResult consumerResult,
        DatagramBulkResult reflectorResult,
        double precisionAllowed)
    {
        double consumerRate = 1000.0 * consumerResult.getCount() / consumerResult.getElapsedMs();
        LOGGER.info("Consumer rate is {} packet/sec", consumerRate);

        Assertions.assertEquals(PACKET_PER_SEC, consumerRate, PACKET_PER_SEC * precisionAllowed);
        Assertions.assertArrayEquals(producerResult.getDigest(), reflectorResult.getDigest());
    }

}
