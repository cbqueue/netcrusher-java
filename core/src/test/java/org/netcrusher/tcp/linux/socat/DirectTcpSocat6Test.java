package org.netcrusher.tcp.linux.socat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DirectTcpSocat6Test extends AbstractTcpSocatTestLinux {

    @Test
    void loop() {
        Assertions.assertDoesNotThrow(() -> loop(SOCAT6_PROCESSOR, SOCAT6_REFLECTOR_DIRECT, DEFAULT_BYTES, FULL_THROUGHPUT));
    }

    @Test
    void direct() {
        Assertions.assertDoesNotThrow(() -> direct(SOCAT6_PRODUCER, SOCAT6_CONSUMER_DIRECT, DEFAULT_BYTES, FULL_THROUGHPUT));
    }

}
