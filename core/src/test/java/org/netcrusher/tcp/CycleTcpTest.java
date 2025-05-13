package org.netcrusher.tcp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.netcrusher.NetCrusherException;
import org.netcrusher.core.reactor.NioReactor;

import java.net.InetSocketAddress;

class CycleTcpTest {

    private static final InetSocketAddress CRUSHER_ADDRESS = new InetSocketAddress("127.0.0.1", 10284);

    private static final InetSocketAddress REFLECTOR_ADDRESS = new InetSocketAddress("127.0.0.1", 10285);

    private NioReactor reactor;

    private TcpCrusher crusher;

    @BeforeEach
    void setUp() throws Exception {
        reactor = new NioReactor();

        crusher = TcpCrusherBuilder.builder()
            .withReactor(reactor)
            .withBindAddress(CRUSHER_ADDRESS)
            .withConnectAddress(REFLECTOR_ADDRESS)
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
    void doubleOpen() {
        Assertions.assertThrows(NetCrusherException.class, () -> crusher.open());
    }

    @Test
    void doubleClose() {
        crusher.close();
        Assertions.assertDoesNotThrow(() -> crusher.close());
    }

    @Test
    void doubleFreeze() {
        crusher.freeze();
        Assertions.assertThrows(NetCrusherException.class, () -> crusher.freeze());
    }

    @Test
    void doubleUnfreeze() {
        crusher.freeze();
        crusher.unfreeze();
        Assertions.assertThrows(NetCrusherException.class, () -> crusher.unfreeze());
    }

    @Test
    void unfreezeWithoutFreeze() {
        Assertions.assertThrows(NetCrusherException.class, () -> crusher.unfreeze());
    }

    @Test
    void reopen() {
        Assertions.assertDoesNotThrow(() -> crusher.reopen());
    }

    @Test
    void reopenAfterClose() {
        crusher.close();
        Assertions.assertThrows(NetCrusherException.class, () -> crusher.reopen());
    }
}
