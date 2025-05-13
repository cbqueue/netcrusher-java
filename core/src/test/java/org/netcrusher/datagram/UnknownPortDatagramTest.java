package org.netcrusher.datagram;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.netcrusher.core.nio.NioUtils;
import org.netcrusher.core.reactor.NioReactor;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

class UnknownPortDatagramTest {

    private static final int PORT_CRUSHER = 10283;

    private static final int PORT_CONNECT = 10284;

    private static final String HOSTNAME = "127.0.0.1";

    private NioReactor reactor;

    private DatagramCrusher crusher;

    @BeforeEach
    void setUp() throws Exception {
        reactor = new NioReactor();

        crusher = DatagramCrusherBuilder.builder()
            .withReactor(reactor)
            .withBindAddress(HOSTNAME, PORT_CRUSHER)
            .withConnectAddress(HOSTNAME, PORT_CONNECT)
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
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(true);
        channel.connect(new InetSocketAddress(HOSTNAME, PORT_CRUSHER));

        try {
            ByteBuffer bb = ByteBuffer.allocate(1024);
            bb.limit(800);
            bb.position(0);

            Assertions.assertEquals(800, channel.write(bb));

            Thread.sleep(1001);
        } finally {
            NioUtils.close(channel);
        }
    }
}
