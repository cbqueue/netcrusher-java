package org.netcrusher.tcp.linux.iperf;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.netcrusher.core.reactor.NioReactor;
import org.netcrusher.tcp.TcpCrusher;
import org.netcrusher.tcp.TcpCrusherBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CrusherTcpIperf4Test extends AbstractTcpIperfTestLinux {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrusherTcpIperf4Test.class);

    private NioReactor reactor;

    private TcpCrusher crusher;

    @BeforeEach
    void setUp() throws Exception {
        reactor = new NioReactor(10);

        crusher = TcpCrusherBuilder.builder()
            .withReactor(reactor)
            .withBindAddress(ADDR_LOOPBACK4, PORT_CRUSHER)
            .withConnectAddress(ADDR_LOOPBACK4, PORT_IPERF)
            .withCreationListener((addr) ->
                LOGGER.info("Client is created <{}>", addr))
            .withDeletionListener((addr, byteMeters) ->
                LOGGER.info("Client is deleted <{}>", addr))
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
    void test() {
        Assertions.assertDoesNotThrow(() -> loop(IPERF_SERVER, IPERF4_CLIENT_PROXIED));
    }
}
