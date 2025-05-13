package org.netcrusher.tcp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.netcrusher.core.reactor.NioReactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

class UnknownHostTcpTest {

    private static final int PORT_CRUSHER = 10080;

    private static final int PORT_CONNECT = 53654;

    private static final String HOSTNAME_BIND = "127.0.0.1";

    private static final String HOSTNAME_CONNECT = "192.168.251.252";

    private NioReactor reactor;

    private TcpCrusher crusher;

    @BeforeEach
    void setUp() throws Exception {
        reactor = new NioReactor();

        crusher = TcpCrusherBuilder.builder()
            .withReactor(reactor)
            .withBindAddress(HOSTNAME_BIND, PORT_CRUSHER)
            .withConnectAddress(HOSTNAME_CONNECT, PORT_CONNECT)
            .withConnectionTimeoutMs(500)
            .withBacklog(100)
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
        SocketChannel channel = SocketChannel.open();

        try (channel) {
            channel.configureBlocking(true);
            channel.connect(new InetSocketAddress(HOSTNAME_BIND, PORT_CRUSHER));
            Assertions.assertEquals(0, crusher.getClientAddresses().size());
            Thread.sleep(3002);
            Assertions.assertEquals(0, crusher.getClientAddresses().size());

            ByteBuffer bb = ByteBuffer.allocate(100);
            bb.put((byte) 0x01);
            bb.flip();

            try {
                channel.write(bb);
                Assertions.fail("Exception is expected");
            } catch (IOException e) {
                //
            }
        }
    }
}
