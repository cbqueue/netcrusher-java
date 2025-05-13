package org.netcrusher.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.netcrusher.core.nio.NioUtils;

import java.net.InetSocketAddress;

class NioUtilsTest {

    @Test
    void testParseAddress() {
        InetSocketAddress addr;

        addr = NioUtils.parseInetSocketAddress("127.0.0.1:80");
        Assertions.assertEquals(new InetSocketAddress("127.0.0.1", 80), addr);

        addr = NioUtils.parseInetSocketAddress("localhost:80");
        Assertions.assertEquals(new InetSocketAddress("localhost", 80), addr);

        addr = NioUtils.parseInetSocketAddress("::1:80");
        Assertions.assertEquals(new InetSocketAddress("0:0:0:0:0:0:0:1", 80), addr);

        addr = NioUtils.parseInetSocketAddress("[0:0:0:0:0:0:0:1]:80");
        Assertions.assertEquals(new InetSocketAddress("0:0:0:0:0:0:0:1", 80), addr);
    }
}
