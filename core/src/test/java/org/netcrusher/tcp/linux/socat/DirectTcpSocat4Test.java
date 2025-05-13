package org.netcrusher.tcp.linux.socat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DirectTcpSocat4Test extends AbstractTcpSocatTestLinux {

    @Test
    void loop() {
        Assertions.assertDoesNotThrow(() -> loop(SOCAT4_PROCESSOR, SOCAT4_REFLECTOR_DIRECT, DEFAULT_BYTES, FULL_THROUGHPUT));
    }

    @Test
    void direct() {
        Assertions.assertDoesNotThrow(() -> direct(SOCAT4_PRODUCER, SOCAT4_CONSUMER_DIRECT, DEFAULT_BYTES, FULL_THROUGHPUT));
    }

}
