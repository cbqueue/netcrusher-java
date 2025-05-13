package org.netcrusher.datagram.linux.socat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.netcrusher.core.reactor.NioReactor;
import org.netcrusher.datagram.DatagramCrusher;
import org.netcrusher.datagram.DatagramCrusherBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.StandardProtocolFamily;

class CrusherDatagramSocat4Test extends AbstractDatagramSocatTestLinux {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrusherDatagramSocat4Test.class);

    private NioReactor reactor;

    private DatagramCrusher crusher;

    @BeforeEach
    void setUp() throws Exception {
        reactor = new NioReactor();

        crusher = DatagramCrusherBuilder.builder()
            .withReactor(reactor)
            .withBindAddress(ADDR_LOOPBACK4, PORT_DIRECT)
            .withConnectAddress(ADDR_LOOPBACK4, PORT_PROXY)
            .withProtocolFamily(StandardProtocolFamily.INET)
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
    void loop() {
        Assertions.assertDoesNotThrow(() -> loop(SOCAT4_PROCESSOR, SOCAT4_REFLECTOR_PROXIED, DEFAULT_BYTES, DEFAULT_THROUGHPUT_KBPERSEC));
    }

    @Test
    void loopSlower() {
        Assertions.assertDoesNotThrow(() -> loop(SOCAT4_PROCESSOR, SOCAT4_REFLECTOR_PROXIED, DEFAULT_BYTES / 10, DEFAULT_THROUGHPUT_KBPERSEC / 10));
    }

    @Test
    void loopSlowest() {
        Assertions.assertDoesNotThrow(() -> loop(SOCAT4_PROCESSOR, SOCAT4_REFLECTOR_PROXIED, DEFAULT_BYTES / 100, DEFAULT_THROUGHPUT_KBPERSEC / 100));
    }

    @Test
    void direct() {
        Assertions.assertDoesNotThrow(() -> direct(SOCAT4_PRODUCER, SOCAT4_CONSUMER_PROXIED, DEFAULT_BYTES, DEFAULT_THROUGHPUT_KBPERSEC));
    }

    @Test
    void directSlower() {
        Assertions.assertDoesNotThrow(() -> direct(SOCAT4_PRODUCER, SOCAT4_CONSUMER_PROXIED, DEFAULT_BYTES / 10, DEFAULT_THROUGHPUT_KBPERSEC / 10));
    }

    @Test
    void directSlowest() {
        Assertions.assertDoesNotThrow(() -> direct(SOCAT4_PRODUCER, SOCAT4_CONSUMER_PROXIED, DEFAULT_BYTES / 100, DEFAULT_THROUGHPUT_KBPERSEC / 100));
    }

}
