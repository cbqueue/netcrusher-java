package org.netcrusher.tcp.bulk;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

class TcpBulkTest {

    private static final int PORT_SERVER = 10082;

    private static final String HOSTNAME = "127.0.0.1";

    private static final long COUNT = 2 * 1_000_000;

    private static final long SEND_WAIT_MS = 20_000;

    private static final long READ_WAIT_MS = 10_000;

    private TcpBulkServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new TcpBulkServer(new InetSocketAddress(HOSTNAME, PORT_SERVER), COUNT);
        server.open();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void test() throws Exception {
        final InetSocketAddress serverAddress = new InetSocketAddress(HOSTNAME, PORT_SERVER);

        try (TcpBulkClient client1 = TcpBulkClient.forAddress("EXT", serverAddress, COUNT)) {
            final byte[] producer1Digest = client1.awaitProducerResult(SEND_WAIT_MS).getDigest();

            Assertions.assertEquals(1, server.getClients().size());
            try (TcpBulkClient client2 = server.getClients().iterator().next()) {
                final byte[] producer2Digest = client2.awaitProducerResult(SEND_WAIT_MS).getDigest();

                final byte[] consumer1Digest = client1.awaitConsumerResult(READ_WAIT_MS).getDigest();
                final byte[] consumer2Digest = client2.awaitConsumerResult(READ_WAIT_MS).getDigest();

                Assertions.assertNotNull(producer1Digest);
                Assertions.assertNotNull(producer2Digest);

                Assertions.assertArrayEquals(producer1Digest, consumer2Digest);
                Assertions.assertArrayEquals(producer2Digest, consumer1Digest);
            }
        }
    }
}
