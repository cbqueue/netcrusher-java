package org.netcrusher.datagram.linux.iperf;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.netcrusher.core.reactor.NioReactor;
import org.netcrusher.datagram.DatagramCrusher;
import org.netcrusher.datagram.DatagramCrusherBuilder;
import org.netcrusher.tcp.TcpCrusher;
import org.netcrusher.tcp.TcpCrusherBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CrusherDatagramIperf4Test extends AbstractDatagramIperfTestLinux {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrusherDatagramIperf4Test.class);

    private NioReactor reactor;

    private DatagramCrusher datagramCrusher;

    private TcpCrusher tcpCrusher;

    @BeforeEach
    void setUp() throws Exception {
        reactor = new NioReactor(10);

        datagramCrusher = DatagramCrusherBuilder.builder()
            .withReactor(reactor)
            .withBindAddress(ADDR_LOOPBACK4, PORT_CRUSHER)
            .withConnectAddress(ADDR_LOOPBACK4, PORT_IPERF)
            .withBufferSize(65535)
            .withCreationListener(addr -> LOGGER.info("Client is created <{}>", addr))
            .withDeletionListener((addr, byteMeters, packetMeters) -> LOGGER.info("Client is deleted <{}>", addr))
            .buildAndOpen();

        // for iperf3 TCP control channel on the same port
        tcpCrusher = TcpCrusherBuilder.builder()
            .withReactor(reactor)
            .withBindAddress(ADDR_LOOPBACK4, PORT_CRUSHER)
            .withConnectAddress(ADDR_LOOPBACK4, PORT_IPERF)
            .withCreationListener(addr -> LOGGER.info("Client is created <{}>", addr))
            .withDeletionListener((addr, byteMeters) -> LOGGER.info("Client is deleted <{}>", addr))
            .buildAndOpen();
    }

    @AfterEach
    void tearDown() {
        if (datagramCrusher != null) {
            datagramCrusher.close();
            Assertions.assertFalse(datagramCrusher.isOpen());
        }

        if (tcpCrusher != null) {
            tcpCrusher.close();
            Assertions.assertFalse(tcpCrusher.isOpen());
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
