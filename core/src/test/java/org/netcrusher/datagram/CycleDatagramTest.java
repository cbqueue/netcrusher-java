package org.netcrusher.datagram;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.netcrusher.NetCrusherException;
import org.netcrusher.core.reactor.NioReactor;

import java.net.InetSocketAddress;

public class CycleDatagramTest {

    private static final InetSocketAddress CRUSHER_ADDRESS = new InetSocketAddress("127.0.0.1", 10284);

    private static final InetSocketAddress REFLECTOR_ADDRESS = new InetSocketAddress("127.0.0.1", 10285);

    private NioReactor reactor;

    private DatagramCrusher crusher;

    @Before
    public void setUp() throws Exception {
        reactor = new NioReactor();

        crusher = DatagramCrusherBuilder.builder()
            .withReactor(reactor)
            .withBindAddress(CRUSHER_ADDRESS)
            .withConnectAddress(REFLECTOR_ADDRESS)
            .buildAndOpen();
    }

    @After
    public void tearDown() throws Exception {
        if (crusher != null) {
            crusher.close();
        }

        if (reactor != null) {
            reactor.close();
        }
    }

    @Test(expected = NetCrusherException.class)
    public void doubleOpen() {
        crusher.open();
    }

    @Test
    public void doubleClose() {
        crusher.close();
        crusher.close();
    }

    @Test(expected = NetCrusherException.class)
    public void doubleFreeze() {
        crusher.freeze();
        crusher.freeze();
    }

    @Test(expected = NetCrusherException.class)
    public void doubleUnfreeze() {
        crusher.freeze();
        crusher.unfreeze();
        crusher.unfreeze();
    }

    @Test(expected = NetCrusherException.class)
    public void unfreezeWithoutFreeze() {
        crusher.unfreeze();
    }

    @Test
    public void reopen() {
        crusher.reopen();
    }

    @Test(expected = NetCrusherException.class)
    public void reopenAfterClose() {
        crusher.close();
        crusher.reopen();
    }
}
