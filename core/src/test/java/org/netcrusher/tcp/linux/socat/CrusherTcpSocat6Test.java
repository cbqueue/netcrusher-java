package org.netcrusher.tcp.linux.socat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.netcrusher.core.reactor.NioReactor;
import org.netcrusher.tcp.TcpCrusher;
import org.netcrusher.tcp.TcpCrusherBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CrusherTcpSocat6Test extends AbstractTcpSocatTestLinux {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrusherTcpSocat6Test.class);

    private NioReactor reactor;

    private TcpCrusher crusher;

    @BeforeEach
    void setUp() throws Exception {
        reactor = new NioReactor();

        crusher = TcpCrusherBuilder.builder()
            .withReactor(reactor)
            .withBindAddress(ADDR_LOOPBACK6, PORT_DIRECT)
            .withConnectAddress(ADDR_LOOPBACK6, PORT_PROXY)
            .withCreationListener(addr -> LOGGER.info("Client is created <{}>", addr))
            .withDeletionListener((addr, byteMeters) -> LOGGER.info("Client is deleted <{}>", addr))
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
        Assertions.assertDoesNotThrow(() -> loop(SOCAT6_PROCESSOR, SOCAT6_REFLECTOR_PROXIED, DEFAULT_BYTES, FULL_THROUGHPUT));
    }

    @Test
    void loopSlower() {
        Assertions.assertDoesNotThrow(() -> loop(SOCAT6_PROCESSOR, SOCAT6_REFLECTOR_PROXIED, DEFAULT_BYTES / 10, DEFAULT_THROUGHPUT_KBPERSEC / 10));
    }

    @Test
    void loopSlowest() {
        Assertions.assertDoesNotThrow(() -> loop(SOCAT6_PROCESSOR, SOCAT6_REFLECTOR_PROXIED, DEFAULT_BYTES / 100, DEFAULT_THROUGHPUT_KBPERSEC / 100));
    }

    @Test
    void direct() {
        Assertions.assertDoesNotThrow(() -> direct(SOCAT6_PRODUCER, SOCAT6_CONSUMER_PROXIED, DEFAULT_BYTES, FULL_THROUGHPUT));
    }

    @Test
    void directSlower() {
        Assertions.assertDoesNotThrow(() -> direct(SOCAT6_PRODUCER, SOCAT6_CONSUMER_PROXIED, DEFAULT_BYTES / 10, DEFAULT_THROUGHPUT_KBPERSEC / 10));
    }

    @Test
    void directSlowest() {
        Assertions.assertDoesNotThrow(() -> direct(SOCAT6_PRODUCER, SOCAT6_CONSUMER_PROXIED, DEFAULT_BYTES / 100, DEFAULT_THROUGHPUT_KBPERSEC / 100));
    }
}
