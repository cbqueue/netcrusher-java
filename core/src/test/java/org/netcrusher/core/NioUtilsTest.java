package org.netcrusher.core;

import org.junit.Assert;
import org.junit.Test;
import org.netcrusher.core.nio.NioUtils;

import java.net.InetSocketAddress;

public class NioUtilsTest {

    @Test
    public void testParseAddress() {
        InetSocketAddress addr;

        addr = NioUtils.parseInetSocketAddress("127.0.0.1:80");
        Assert.assertEquals(new InetSocketAddress("127.0.0.1", 80), addr);

        addr = NioUtils.parseInetSocketAddress("localhost:80");
        Assert.assertEquals(new InetSocketAddress("localhost", 80), addr);

        addr = NioUtils.parseInetSocketAddress("::1:80");
        Assert.assertEquals(new InetSocketAddress("0:0:0:0:0:0:0:1", 80), addr);

        addr = NioUtils.parseInetSocketAddress("[0:0:0:0:0:0:0:1]:80");
        Assert.assertEquals(new InetSocketAddress("0:0:0:0:0:0:0:1", 80), addr);
    }
}
