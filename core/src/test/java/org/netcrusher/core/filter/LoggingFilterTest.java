package org.netcrusher.core.filter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

class LoggingFilterTest {

    @Test
    void test() {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8080);

        LoggingFilter filter = new LoggingFilter(address, "dump.test", LoggingFilter.Level.INFO);

        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.put(new byte[] { (byte) 1, (byte) 2, (byte) 44, (byte) 0xFF });
        bb.flip();

        Assertions.assertDoesNotThrow(() -> filter.transform(bb));
    }
}
