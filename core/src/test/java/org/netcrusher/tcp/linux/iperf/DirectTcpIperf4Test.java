package org.netcrusher.tcp.linux.iperf;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DirectTcpIperf4Test extends AbstractTcpIperfTestLinux {

    @Test
    void test() {
        Assertions.assertDoesNotThrow(() -> loop(IPERF_SERVER, IPERF4_CLIENT_DIRECT));
    }
}
