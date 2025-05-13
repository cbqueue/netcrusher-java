package org.netcrusher.datagram.linux.iperf;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DirectDatagramIperf4Test extends AbstractDatagramIperfTestLinux {

    @Test
    void test() {
        Assertions.assertDoesNotThrow(() -> loop(IPERF_SERVER, IPERF4_CLIENT_DIRECT));
    }
}
